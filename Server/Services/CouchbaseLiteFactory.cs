using Couchbase.Lite;
using Couchbase.Lite.Query;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Kordata.AccessBridge.Server
{

    public interface ICouchbaseLiteFactory
    {
        Database GetDatabase();
    }

    public class CouchbaseLiteFactory : ICouchbaseLiteFactory
    {
        private readonly CouchbaseConfig config;
        private readonly ILogger logger;

        public CouchbaseLiteFactory(IOptions<CouchbaseConfig> options, ILogger<CouchbaseLiteFactory> logger,
            ILogger<Database> dbLogger)
        {
            this.logger = logger;
            config = options.Value;

            Database.Log.Custom = new CbLogger(dbLogger);
            CreateIndexes();

            logger.LogDebug("Initialized Couchbase connection factory for database {Database}", config.Name);
        }

        private void CreateIndexes()
        {
            logger.LogTrace("Creating Indexes");
            
            var dbIndex = IndexBuilder.ValueIndex(
                ValueIndexItem.Expression(Expression.Property("database")));

            using (var db = new Database(config.Name))
            {
                db.CreateIndex("DatabaseIndex", dbIndex);
            }
        }

        public Database GetDatabase()
        {
            return new Database(config.Name);
        }

        private class CbLogger : Couchbase.Lite.Logging.ILogger
        {
            private readonly ILogger logger;
            public CbLogger(ILogger logger)
            {
                this.logger = logger;
            }

            public Couchbase.Lite.Logging.LogLevel Level
            {
                get
                {
                    if (logger.IsEnabled(LogLevel.Trace)) return Couchbase.Lite.Logging.LogLevel.Verbose;
                    if (logger.IsEnabled(LogLevel.Debug)) return Couchbase.Lite.Logging.LogLevel.Debug;
                    if (logger.IsEnabled(LogLevel.Information)) return Couchbase.Lite.Logging.LogLevel.Info;
                    if (logger.IsEnabled(LogLevel.Warning)) return Couchbase.Lite.Logging.LogLevel.Warning;
                    if (logger.IsEnabled(LogLevel.Error)) return Couchbase.Lite.Logging.LogLevel.Error;
                    return Couchbase.Lite.Logging.LogLevel.None;
                }
            }

            public void Log(Couchbase.Lite.Logging.LogLevel level, Couchbase.Lite.Logging.LogDomain domain, string message)
            {
                logger.Log(GetLevel(level), $"[{domain.ToString()}] {message}");
            }

            private LogLevel GetLevel(Couchbase.Lite.Logging.LogLevel level)
            {
                switch (level)
                {
                    case Couchbase.Lite.Logging.LogLevel.Verbose: return LogLevel.Trace;
                    case Couchbase.Lite.Logging.LogLevel.Debug: return LogLevel.Debug;
                    case Couchbase.Lite.Logging.LogLevel.Info: return LogLevel.Information;
                    case Couchbase.Lite.Logging.LogLevel.Warning: return LogLevel.Warning;
                    case Couchbase.Lite.Logging.LogLevel.Error: return LogLevel.Error;
                    case Couchbase.Lite.Logging.LogLevel.None: return LogLevel.None;
                    default: return LogLevel.None;
                }
            }
        }
    }
}