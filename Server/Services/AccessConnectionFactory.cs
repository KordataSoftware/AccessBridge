using System.Data.Odbc;
using System.IO;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Kordata.AccessBridge.Server
{
    public interface IAccessConnectionFactory
    {
        bool DatabaseExists(string database);
        OdbcConnection CreateConnection(string database, string username = null,
            string password = null);
    }

    public class AccessConnectionFactory : IAccessConnectionFactory
    {
        private readonly AccessConfig config;
        private readonly ILogger logger;
        public AccessConnectionFactory(IOptions<AccessConfig> options, ILogger<AccessConnectionFactory> logger)
        {
            this.logger = logger;
            config = options.Value;

            logger.LogDebug("Initialized Access connection factory for directory {Directory}", config.DatabaseDirectory);
        }

        public bool DatabaseExists(string database)
        {
            return File.Exists(Path.Combine(config.DatabaseDirectory, $"{database}.accdb"));
        }

        public OdbcConnection CreateConnection(string database, string username = null,
            string password = null)
        {
            logger.LogDebug("Creating connection to database {Database} for User {Username}", database, username);
            
            var connectionString = GetConnectionString(database, username, password);

            logger.LogTrace("Connection String: {ConnectionString}", 
                password != null ? connectionString.Replace(password, "*******") : connectionString);

            return new OdbcConnection(connectionString);
        }

        private string GetConnectionString(string database, string username = null,
            string password = null)
        {
            var path = Path.Combine(config.DatabaseDirectory, $"{database}.accdb");
            var connectionString = $"Driver={{Microsoft Access Driver (*.mdb, *.accdb)}};Dbq={path}";

            if (username != null && password != null)
            {
                connectionString += $";Uid={username};Pwd={password};";
            }

            return connectionString;
        }
    }
}