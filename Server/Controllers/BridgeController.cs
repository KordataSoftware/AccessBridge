using System;
using System.Collections.Generic;
using System.Data.Odbc;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using MoreLinq;
using Newtonsoft.Json.Linq;

namespace Kordata.AccessBridge.Server
{
    public class BridgeController : BaseController
    {
        public BridgeController(ILogger<BridgeController> logger,
            IAccessConnectionFactory connection)
            : base(logger, connection)
        {

        }

        [HttpGet("/v1/{database}/{table}")]
        public Task<IActionResult> GetRecords(string database, string table)
        {
            return
                ValidateDatabase(database, () =>
                WithQuery(table, Request.Query, query =>
                WithCommand(database, query, command =>
                WithReader(command, async reader =>
                {
                    var response = new JArray();

                    while (await reader.ReadAsync())
                    {
                        response.Add(reader.GetJObject());
                    }

                    return new JsonResult(response);
                }))));
        }

        [HttpPost("/v1/{database}/{table}")]
        public async Task<IActionResult> PostRecords(string database, string table, 
            [FromQuery]string primaryKey, [FromBody]JArray records)
        {
            if (records == null || records.Count == 0) return NoContent();

            return await
                ValidateDatabase(database, () =>
                WithConnection(database, async connection =>
                {
                    var results = await InsertOrUpdateAsync(table, records, primaryKey, connection);

                    return new JsonResult(results);
                }));
        }

        private async Task<JArray> InsertOrUpdateAsync(string table, JArray records, string primaryKey, OdbcConnection connection)
        {
            using (var checkCommand = connection.CreateCommand())
            using (var insertCommand = connection.CreateCommand())
            using (var updateCommand = connection.CreateCommand())
            {
                checkCommand.CommandText = $"SELECT Count([{primaryKey}]) FROM [{table}] WHERE [{primaryKey}] = ?";
                checkCommand.Prepare();

                var columns = GetInsertColumns((JObject)records[0]);
                var columnFragment = string.Join(",", columns.Select(c => $"[{c}]"));
                var parmFragment = string.Join(",", columns.Select(c => "?"));
                insertCommand.CommandText = $"INSERT INTO {table} ({columnFragment}) VALUES ({parmFragment})";
                insertCommand.Prepare();

                var updateFragment = string.Join(",", columns.Where(c => c != primaryKey).Select(c => $"[{c}] = ?"));
                updateCommand.CommandText = $"UPDATE {table} SET {updateFragment} WHERE {primaryKey} = ?";
                updateCommand.Prepare();

                var results = new JArray();

                foreach (JObject record in records)
                {
                    var rowsAffected = -1;
                    if (await RecordExistsAsync(record, primaryKey, checkCommand))
                    {
                        rowsAffected = await UpdateRecordAsync(record, primaryKey, updateCommand);
                    }
                    else
                    {
                        rowsAffected = await InsertRecordAsync(record, insertCommand);
                    }

                    var rowResult = new JObject();
                    rowResult["success"] = rowsAffected > 0;
                    results.Add(rowResult);
                }

                return results;
            }
        }

        private static async Task<bool> RecordExistsAsync(JObject record, string primaryKey, OdbcCommand command)
        {
            if (record[primaryKey] == null) return false;
            
            var pKey = (JValue)record[primaryKey];
                
            command.Parameters.Add($"@{primaryKey}", pKey.Type.ToOdbcType()).Value = pKey.Value;

            var count = (int) await command.ExecuteScalarAsync();
            return count > 0;
        }

        private static async Task<int> UpdateRecordAsync(JObject record, string primaryKey, OdbcCommand updateCommand)
        {
            updateCommand.Parameters.Clear();
            record.Properties()
                .Where(p => p.Name != primaryKey)
                .Select(p => (Name: p.Name, Value: (JValue)p.Value))
                .ForEach(p => updateCommand.Parameters.Add($"@{p.Name}", p.Value.Type.ToOdbcType()).Value = p.Value.Value);

            var pKeyValue = (JValue)record[primaryKey];
            updateCommand.Parameters.Add($"@[{primaryKey}]", pKeyValue.Type.ToOdbcType()).Value = pKeyValue.Value;

            return await updateCommand.ExecuteNonQueryAsync();
        }

        private static Task<int> InsertRecordAsync(JObject record, OdbcCommand insertCommand)
        {
            insertCommand.Parameters.Clear();
            record.Properties()
                .Select(p => (Name: p.Name, Value: (JValue)p.Value))
                .ForEach(p => 
                {
                    insertCommand.Parameters.Add($"@{p.Name}", p.Value.Type.ToOdbcType()).Value = ((JValue)record[p.Name]).Value;
                });

            return insertCommand.ExecuteNonQueryAsync();
        }

        private List<string> GetInsertColumns(JObject obj)
        {
            return obj
                .Properties()
                .Select(p => p.Name)
                .ToList();
        }

        private Task<IActionResult> WithQuery(string table, IQueryCollection requestQuery, Func<Query, Task<IActionResult>> then)
        {
            var query = new Query
            {
                Command = $"SELECT * FROM {table}"
            };

            return then?.Invoke(query);
        }
    }
}