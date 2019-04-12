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
    public class BasicOperationController : BaseController
    {
        public BasicOperationController(ILogger<BasicOperationController> logger,
            IAccessConnectionFactory connectionFactory)
            : base(logger, connectionFactory)
        {
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
                    var response = new JObject();
                    response["schema"] = reader.GetSchemaJArray();

                    var results = new JArray();
                    while (await reader.ReadAsync())
                    {
                        results.Add(reader.GetJObject());
                    }

                    response["results"] = results;

                    return new JsonResult(response);
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
    }
}