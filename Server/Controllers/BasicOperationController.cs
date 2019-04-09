using System.Data.Odbc;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using MoreLinq;
using Newtonsoft.Json.Linq;
using System;
using System.Data.Common;

namespace Kordata.AccessBridge.Server
{
    public class BasicOperationController : Controller
    {
        private readonly IAccessConnectionFactory connectionFactory;

        public BasicOperationController(IAccessConnectionFactory connectionFactory)
        {
            this.connectionFactory = connectionFactory;
        }

        [HttpGet("/v1/{database}/health_check")]
        public Task<IActionResult> HealthCheck(string database)
        {
            return
                ValidateDatabase(database, () =>
                {
                    return Task.FromResult((IActionResult)Ok());
                });
        }

        [HttpPost("/v1/{database}/query")]
        public Task<IActionResult> Query(string database, [FromBody]Query query)
        {
            return
                ValidateDatabase(database, () =>
                ValidateQuery(query, () =>
                WithCommand(database, query, command =>
                WithReader(command, async reader =>
                {
                    var results = new JArray();
                    while (await reader.ReadAsync())
                    {
                        results.Add(reader.GetJObject());
                    }

                    return new JsonResult(results);
                }))));
        }

        [HttpPost("/v1/{database}/mutate")]
        public Task<IActionResult> Mutate(string database, [FromBody]Query query)
        {
            return
                ValidateDatabase(database, () =>
                ValidateQuery(query, () =>
                WithCommand(database, query, async command =>
                {
                    var rowsAffected = await command.ExecuteNonQueryAsync();

                    return new JsonResult(new JObject(new JProperty("rowsAffected", rowsAffected)));
                })));
        }

        private async Task<IActionResult> ValidateDatabase(string database, Func<Task<IActionResult>> then)
        {
            if (string.IsNullOrEmpty(database)) return BadRequest();
            if (!connectionFactory.DatabaseExists(database)) return NotFound();

            return await then?.Invoke();
        }

        private async Task<IActionResult> ValidateQuery(Query query, Func<Task<IActionResult>> then)
        {
            if (query == null) return BadRequest();
            if (string.IsNullOrEmpty(query.Command)) return BadRequest();

            return await then?.Invoke();
        }

        private async Task<IActionResult> WithCommand(string database, Query query, Func<OdbcCommand, Task<IActionResult>> then)
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

        private async Task<IActionResult> WithReader(OdbcCommand command, Func<DbDataReader, Task<IActionResult>> then)
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
                return BadRequest(e.Message);
            }
        }
    }
}