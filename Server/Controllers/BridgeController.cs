using System;
using System.Collections.Generic;
using System.Data.Odbc;
using System.Data;
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
            using (var tableSchemaCommand = connection.CreateCommand())
            {
                tableSchemaCommand.CommandText = $"SELECT TOP 1 * FROM {table}";
                tableSchemaCommand.Prepare();
                var tableSchemaCommandReader = await tableSchemaCommand.ExecuteReaderAsync();
                var tableSchema = tableSchemaCommandReader.GetSchemaJArray();
                tableSchemaCommandReader.Close();
                
                checkCommand.CommandText = $"SELECT Count([{primaryKey}]) FROM [{table}] WHERE [{primaryKey}] = ?";
                checkCommand.Prepare();

                var columns = GetInsertColumns((JObject)records[0]);
                var columnFragment = string.Join(",", columns.Select(c => $"[{c}]"));
                var parmFragment = string.Join(",", columns.Select(c => "?"));
                insertCommand.CommandText = $"INSERT INTO {table} ({columnFragment}) VALUES ({parmFragment})";
                insertCommand.Prepare();

                var results = new JArray();

                foreach (JObject record in records)
                {
                    var success = false;
                    if (await RecordExistsAsync(record, primaryKey, checkCommand))
                    {
                        success = await UpdateRecordAsync(table, tableSchema, columns, record, primaryKey, updateCommand);
                    }
                    else
                    {
                        success = await InsertRecordAsync(tableSchema, record, insertCommand);
                    }

                    var rowResult = new JObject();
                    rowResult["success"] = success;
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

        private static async Task<bool> UpdateRecordAsync(string table, JArray tableSchema, List<string> columns, 
            JObject record, string primaryKey, OdbcCommand updateCommand)
        {
            var propertyBatches = record.Properties()
                .Where(p => p.Name != primaryKey)
                .Select(p => (Name: p.Name, Value: (JValue)p.Value))
                .Batch(127);
            
            var tasks = propertyBatches.Select(
                batch => UpdatePartialRecordAsync(table, tableSchema, columns, primaryKey, (JValue)record[primaryKey], batch, updateCommand)
            );

            var success = true;
            foreach (var task in tasks)
            {
                var result = await task;
                if (!result)
                {
                    success = false;
                }
            }

            return success;
        }

        private static async Task<bool> UpdatePartialRecordAsync(string table, JArray tableSchema, List<string> columns, string pKeyName,  
            JValue pKeyValue, IEnumerable<(string Name, JValue Value)> properties, OdbcCommand updateCommand)
        {
            updateCommand.Parameters.Clear();

            var updateFragment = string.Join(",", columns.Where(c => c != pKeyName && properties.Any(p => p.Name == c)).Select(c => $"[{c}] = ?"));
            updateCommand.CommandText = $"UPDATE {table} SET {updateFragment} WHERE {pKeyName} = ?";
            updateCommand.Prepare();

            properties
                .ForEach(p => 
                {
                    var parameter = updateCommand.Parameters.Add($"@{p.Name}", p.Value.Type.ToOdbcType());

                    var columnSchema = tableSchema.First(c => (string)c["columnName"] == p.Name);

                    if ((string)columnSchema["dataType"] == "string")
                    {
                        parameter.Size = (int)columnSchema["columnSize"];
                    }

                    parameter.Value = p.Value.Value;
                });

            updateCommand.Parameters.Add($"@[{pKeyName}]", pKeyValue.Type.ToOdbcType()).Value = pKeyValue.Value;

            var result = await updateCommand.ExecuteNonQueryAsync();

            return result > 0;
        }

        private static async Task<bool> InsertRecordAsync(JArray tableSchema, JObject record, OdbcCommand insertCommand)
        {
            insertCommand.Parameters.Clear();
            record.Properties()
                .Select(p => (Name: p.Name, Value: (JValue)p.Value))
                .ForEach(p => 
                {
                    var parameter = insertCommand.Parameters.Add($"@{p.Name}", p.Value.Type.ToOdbcType());

                    var columnSchema = tableSchema.First(c => (string)c["columnName"] == p.Name);

                    if ((string)columnSchema["dataType"] == "string")
                    {
                        parameter.Size = (int)columnSchema["columnSize"];
                    }

                    parameter.Value = ((JValue)record[p.Name]).Value;

                });

            var rowsEffected = await insertCommand.ExecuteNonQueryAsync();

            return rowsEffected > 0;
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