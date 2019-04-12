
using Couchbase.Lite;
using Couchbase.Lite.Query;
using Microsoft.Extensions.Configuration;

namespace Kordata.AccessBridge.Server
{

    public interface ICouchbaseLiteFactory
    {
        Database GetDatabase();
    }

    public class CouchbaseLiteFactory : ICouchbaseLiteFactory
    {
        private readonly string databaseName;

        public CouchbaseLiteFactory(IConfiguration configuration)
        {
            databaseName = configuration.GetValue<string>("Name");
            Couchbase.Lite.Support.NetDesktop.Activate();
            CreateIndexes();
        }

        private void CreateIndexes()
        {
            var dbIndex = IndexBuilder.ValueIndex(
                ValueIndexItem.Expression(Expression.Property("database")));

            using (var db = new Database(databaseName))
            {
                db.CreateIndex("DatabaseIndex", dbIndex);
            }
        }

        public Database GetDatabase()
        {
            return new Database(databaseName);
        }
    }
}