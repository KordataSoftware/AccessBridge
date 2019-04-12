using System;
using System.Collections.Generic;
using Couchbase.Lite;
using Couchbase.Lite.Query;
using Newtonsoft.Json.Linq;
using NodaTime;

namespace Kordata.AccessBridge.Server
{
    public class Watch
    {
        public string Id { get; set; }
        public string Database { get; set; }
        public string Table { get; set; }
        public string TimestampColumn { get; set; }
        public string WebhookUri { get; set; }
        public Instant State { get; set; }

        public Watch() { }

        public Watch(IDictionaryObject result)
        {
            Id = result.GetString("id");
            Database = result.GetString("database");
            Table = result.GetString("table");
            TimestampColumn = result.GetString("timestampColumn");
            WebhookUri = result.GetString("webhookUri");
            State = Instant.FromDateTimeOffset(result.GetDate("state"));
        }

        public void SaveToDocument(MutableDocument doc)
        {
            doc.SetString("id", Id);
            doc.SetString("database", Database);
            doc.SetString("table", Table);
            doc.SetString("timestampColumn", TimestampColumn);
            doc.SetString("webhookUri", WebhookUri);
            doc.SetDate("state", State.ToDateTimeOffset());
        }
    }
}