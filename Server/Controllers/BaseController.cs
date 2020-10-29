using System.Data.Odbc;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using MoreLinq;
using Newtonsoft.Json.Linq;
using System;
using System.Data.Common;
using Microsoft.Extensions.Logging;

namespace Kordata.AccessBridge.Server
{
    public abstract class BaseController : Controller
    {
        protected readonly ILogger logger;
        private readonly IAccessConnectionFactory connectionFactory;

        public BaseController(ILogger logger, IAccessConnectionFactory connectionFactory)
        {
            this.logger = logger;
            this.connectionFactory = connectionFactory;
        }

        protected async Task<IActionResult> ValidateDatabase(string database, Func<Task<IActionResult>> then)
        {
            if (string.IsNullOrEmpty(database)) return BadRequest();
            if (!connectionFactory.DatabaseExists(database)) return NotFound();

            return await then?.Invoke();
        }

        protected async Task<IActionResult> ValidateQuery(Query query, Func<Task<IActionResult>> then)
        {
            if (query == null) return BadRequest();
            if (string.IsNullOrEmpty(query.Command)) return BadRequest();

            return await then?.Invoke();
        }

        protected async Task<IActionResult> WithConnection(string database, Func<OdbcConnection, Task<IActionResult>> then)
        {
            using (var connection = connectionFactory.CreateConnection(database))
            {
                if (connection == null)
                {
                    logger.LogWarning("Connection came back null.");
                    return BadRequest();
                }

                try
                {
                    await connection.OpenAsync();
                    logger.LogTrace("Connection open");

                    return await then?.Invoke(connection);
                }
                catch (OdbcException e)
                {
                    logger.LogError(e.Message);
                    return BadRequest(e.Message);
                }
            }
        }

        protected async Task<IActionResult> WithCommand(string database, Query query, Func<OdbcCommand, Task<IActionResult>> then)
        {
            using (var connection = connectionFactory.CreateConnection(database))
            {
                if (connection == null)
                {
                    logger.LogWarning("Connection came back null.");
                    return BadRequest();
                }

                try
                {
                    await connection.OpenAsync();
                    logger.LogTrace("Connection open");

                    using (var command = connection.CreateCommand())
                    {
                        logger.LogDebug("Creating command {Command}", query.Command);
                        command.CommandText = query.Command;

                        if (query.Parameters != null)
                        {
                            query.Parameters.ForEach((parm, i) => command.Parameters.AddWithValue(i.ToString(), parm));
                        }

                        return await then?.Invoke(command);
                    }
                }
                catch (OdbcException e)
                {
                    logger.LogError(e.Message);
                    return BadRequest(e.Message);
                }
            }
        }

        protected async Task<IActionResult> WithReader(OdbcCommand command, Func<DbDataReader, Task<IActionResult>> then)
        {
            try
            {
                logger.LogDebug("Getting reader");
                var reader = await command.ExecuteReaderAsync();

                var result = await then?.Invoke(reader);

                reader.Close();

                return result;
            }
            catch (OdbcException e)
            {
                logger.LogError(e.Message);

                if (e.Message.Contains("42S02")) return NotFound();
                return BadRequest(e.Message);
            }
        }
    }
}