using System;
using System.Collections.Generic;
using Couchbase.Lite;
using Couchbase.Lite.Query;
using System.Linq;
using Newtonsoft.Json.Linq;
using NodaTime;

namespace Kordata.AccessBridge.Server
{
    public interface IWatchRepository
    {
        Watch Get(string id);
        List<Watch> GetForDatabase(string database);
        List<Watch> GetAll();
        string Put(Watch watch);

        bool Delete(string id);
        bool DeleteForDatabase(string database);
    }

    public class WatchRepository : IWatchRepository, IDisposable
    {
        private readonly Database database;
        public WatchRepository(ICouchbaseLiteFactory couchbaseLiteFactory)
        {
            database = couchbaseLiteFactory.GetDatabase();
        }

        public Watch Get(string id)
        {
            var doc = database.GetDocument(id);

            if (doc == null) return null;

            return new Watch(doc);
        }

        public bool Delete(string id)
        {
            var doc = database.GetDocument(id);

            if (doc == null) return false;

            database.Delete(doc);

            return true;
        }

        public List<Watch> GetForDatabase(string database)
        {
            using (var query = QueryBuilder
                .Select(SelectResult.All())
                .From(DataSource.Database(this.database))
                .Where(Expression.Property("database").EqualTo(Expression.String(database))))
            {
                return query
                    .Execute()
                    .Select(r => new Watch(r.GetDictionary(0)))
                    .ToList();
            }
        }

        public bool DeleteForDatabase(string database)
        {
            using (var query = QueryBuilder
                .Select(SelectResult.Property("id"))
                .From(DataSource.Database(this.database))
                .Where(Expression.Property("database").EqualTo(Expression.String(database))))
            {
                query
                    .Execute()
                    .Select(r => r.GetString("id"))
                    .Select(id => this.database.GetDocument(id))
                    .ToList()
                    .ForEach(doc => this.database.Delete(doc));

                return true;
            }
        }

        public List<Watch> GetAll()
        {
            using (var query = QueryBuilder
                .Select(SelectResult.All())
                .From(DataSource.Database(database)))
            {
                return query
                    .Execute()
                    .Select(r => new Watch(r.GetDictionary(0)))
                    .ToList();
            }
        }

        public string Put(Watch watch)
        {
            if (watch.State == null)
            {
                watch.State = Instant.MinValue;
            }

            MutableDocument doc;
            var existingDoc = database.GetDocument(watch.Id);
            if (existingDoc != null)
            {
                doc = existingDoc.ToMutable();
            }
            else
            {
                doc = new MutableDocument(watch.Id);
            }

            watch.SaveToDocument(doc);
            database.Save(doc);

            return watch.Id;
        }

        public void Dispose()
        {
            database.Dispose();
        }
    }
}