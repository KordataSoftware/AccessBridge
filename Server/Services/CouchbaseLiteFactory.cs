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

        public CouchbaseLiteFactory(IOptions<CouchbaseConfig> options, ILogger<CouchbaseLiteFactory> logger)
        {
            this.logger = logger;
            config = options.Value;
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
    }
}