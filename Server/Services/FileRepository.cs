using System.IO;
using System.Threading.Tasks;
using Microsoft.Extensions.Options;
using Microsoft.Extensions.Logging;

namespace Kordata.AccessBridge.Server
{
    public interface IFileRepository
    {
        Task PutAsync(Stream data, string bucket, string key);
    }

    public class FileRepository : IFileRepository
    {
        private readonly FileConfig config;
        private readonly ILogger logger;
        public FileRepository(IOptions<FileConfig> options, ILogger<FileRepository> logger)
        {
            this.logger = logger;
            config = options.Value;
        }

        public async Task PutAsync(Stream data, string bucket, string key)
        {
            var bucketDir = Path.Combine(config.Directory, bucket);
            Directory.CreateDirectory(bucketDir);

            var path = Path.Combine(bucketDir, key);
            
            using (var fs = new FileStream(path, FileMode.OpenOrCreate))
            {
                await data.CopyToAsync(fs);
            }
        }
    }
}