using System.Data.Odbc;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using MoreLinq;
using Newtonsoft.Json.Linq;
using System;
using System.Data.Common;
using Microsoft.Extensions.Hosting;
using System.Threading;
using Microsoft.Extensions.Logging;
using System.Net.Http;
using NodaTime;
using System.Net.Http.Headers;
using System.Text;

namespace Kordata.AccessBridge.Server
{
    public class TableWatcher : IHostedService, IDisposable
    {
        private const int UpdateDelay = 20;
        private readonly IHttpClientFactory httpClientFactory;
        private readonly IAccessConnectionFactory connectionFactory;
        private readonly IWatchRepository watchRepository;

        private readonly ILogger logger;
        private readonly DateTimeZone systemTimeZone;

        private Timer timer;

        public TableWatcher(ILogger<TableWatcher> logger,
            IAccessConnectionFactory connectionFactory,
            IWatchRepository watchRepository,
            IHttpClientFactory httpClientFactory)
        {
            this.httpClientFactory = httpClientFactory;
            this.connectionFactory = connectionFactory;
            this.watchRepository = watchRepository;
            this.logger = logger;
            this.systemTimeZone = DateTimeZoneProviders.Tzdb.GetSystemDefault();
        }

        public Task StartAsync(CancellationToken cancellationToken)
        {
            logger.LogInformation("TableWatcher service is starting.");

            timer = new Timer(DoWork, null, TimeSpan.Zero, TimeSpan.FromSeconds(UpdateDelay));

            return Task.CompletedTask;
        }

        public Task StopAsync(CancellationToken cancellationToken)
        {
            logger.LogInformation("TableWatcher service is stopping.");

            timer.Change(Timeout.Infinite, 0);

            return Task.CompletedTask;
        }

        private void DoWork(object state)
        {
            // For each group, connect to database and check the tables.
            logger.LogInformation("TableWatcher is checking watches for changes.");

            // Load all the watches grouped by database.
            watchRepository.GetAll()
                .GroupBy(w => w.Database)
                .ForEach(g => ProcessGroup(g));
        }

        private void ProcessGroup(IGrouping<string, Watch> watchGroup)
        {
            logger.LogDebug($"TableWatcher is processing watches for database {watchGroup.Key}");
            using (var connection = connectionFactory.CreateConnection(watchGroup.Key))
            {
                if (connection == null)
                {
                    logger.LogWarning("Could not connect to database " + watchGroup.Key);
                    return;
                }

                try
                {
                    connection.Open();

                    watchGroup.ForEach(w => ProcessWatch(w, connection));
                }
                catch (Exception e)
                {
                    logger.LogWarning($"Exception encountered for group {watchGroup.Key}:\n{e.Message}");
                }
            }
        }

        private void ProcessWatch(Watch watch, OdbcConnection connection)
        {
            var results = new JArray();
            var stateInLocalTime = watch.State.InZone(systemTimeZone).LocalDateTime;
            
            logger.LogDebug($"Watch {watch.Id} checking for new changes. Current state is {stateInLocalTime.ToString()}");
            
            using (var command = connection.CreateCommand())
            {
                command.CommandText = $"SELECT * FROM {watch.Table} WHERE {watch.TimestampColumn} > #{stateInLocalTime.ToAccessDateTime()}#";


                using (var reader = command.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        results.Add(reader.GetJObject());
                    }
                }
            }

            if (results.Count == 0)
            {
                logger.LogDebug($"No new results for watch {watch.Id}");
                return;
            }

            Task.Run(() => PostResultsToWebhookAsync(watch, results));
        }

        private async Task PostResultsToWebhookAsync(Watch watch, JArray results)
        {
            logger.LogDebug($"Watch {watch.Id} has {results.Count} changes. Pushing to {watch.WebhookUri}");
            
            var newState = SystemClock.Instance.GetCurrentInstant();
            var client = httpClientFactory.CreateClient("WatchWebhooks");
            var content = new StringContent(results.ToString(), Encoding.UTF8, "application/json");

            var response = await client.PostAsync(watch.WebhookUri, content);

            if (response.IsSuccessStatusCode)
            {
                logger.LogInformation($"Successfully pushed {results.Count} changes to {watch.WebhookUri} for watch {watch.Id}. Updating state to {newState.ToString()}");
                watch.State = newState;
                watchRepository.Put(watch);
            }
            else
            {
                logger.LogWarning($"Failed to push watch changes to webhook {watch.WebhookUri}:\n{response.StatusCode}: {response.ReasonPhrase}");
            }
        }

        public void Dispose()
        {
            timer?.Dispose();
        }
    }
}