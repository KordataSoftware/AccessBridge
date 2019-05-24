using System;
using System.Collections.Generic;
using Couchbase.Lite;
using Couchbase.Lite.Query;
using System.Linq;
using Newtonsoft.Json.Linq;
using NodaTime;
using System.IO;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;

namespace Kordata.AccessBridge.Server
{
    public interface IFileRepository
    {
        Task PutAsync(Stream data, string bucket, string key);
    }

    public class FileRepository : IFileRepository
    {
        private readonly string baseFilePath;

        public FileRepository(IConfiguration configuration)
        {
            baseFilePath = configuration.GetValue<string>("Directory");
        }

        public async Task PutAsync(Stream data, string bucket, string key)
        {
            var bucketDir = Path.Combine(baseFilePath, bucket);
            Directory.CreateDirectory(bucketDir);

            var path = Path.Combine(bucketDir, key);
            
            using (var fs = new FileStream(path, FileMode.OpenOrCreate))
            {
                await data.CopyToAsync(fs);
            }
        }
    }
}