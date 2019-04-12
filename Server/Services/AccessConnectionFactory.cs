using System.Data.Odbc;
using System.IO;
using Microsoft.Extensions.Configuration;

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
        private readonly string databaseLocation;

        public AccessConnectionFactory(IConfiguration configuration)
        {
            this.databaseLocation = configuration.GetValue<string>("DatabaseDirectory");
        }

        public bool DatabaseExists(string database)
        {
            return File.Exists(Path.Combine(databaseLocation, $"{database}.accdb"));
        }

        public OdbcConnection CreateConnection(string database, string username = null,
            string password = null)
        {
            return new OdbcConnection(GetConnectionString(database, username, password));
        }

        private string GetConnectionString(string database, string username = null,
            string password = null)
        {
            var path = Path.Combine(databaseLocation, $"{database}.accdb");
            var connectionString = $"Driver={{Microsoft Access Driver (*.mdb, *.accdb)}};Dbq={path}";

            if (username != null && password != null)
            {
                connectionString += $";Uid={username};Pwd={password};";
            }

            return connectionString;
        }
    }
}