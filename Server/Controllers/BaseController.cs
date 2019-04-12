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
        private readonly ILogger logger;
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
                if (connection == null) return BadRequest();

                try
                {
                    await connection.OpenAsync();

                    return await then?.Invoke(connection);
                }
                catch (OdbcException e)
                {
                    return BadRequest(e.Message);
                }
            }
        }

        protected async Task<IActionResult> WithCommand(string database, Query query, Func<OdbcCommand, Task<IActionResult>> then)
        {
            using (var connection = connectionFactory.CreateConnection(database))
            {
                if (connection == null) return BadRequest();

                try
                {
                    await connection.OpenAsync();

                    using (var command = connection.CreateCommand())
                    {
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
                    return BadRequest(e.Message);
                }
            }
        }

        protected async Task<IActionResult> WithReader(OdbcCommand command, Func<DbDataReader, Task<IActionResult>> then)
        {
            try
            {
                var reader = await command.ExecuteReaderAsync();

                var result = await then?.Invoke(reader);

                reader.Close();

                return result;
            }
            catch (OdbcException e)
            {
                if (e.Message.Contains("42S02")) return NotFound();
                return BadRequest(e.Message);
            }
        }
    }
}