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
    public class FileUploadController : Controller
    {
        private readonly ILogger logger;
        private readonly IFileRepository fileRepository;

        public FileUploadController(ILogger<FileUploadController> logger,
            IFileRepository fileRepository)
        {
            this.logger = logger;
            this.fileRepository = fileRepository;
        }

        [HttpPut("v1/files/{bucket}/{fileName}")]
        [RequestSizeLimit(100_000_000)]
        public async Task<IActionResult> PutFile(string bucket, string fileName)
        {
            try
            {
                await fileRepository.PutAsync(Request.Body, bucket, fileName);
                return NoContent();
            }
            catch (Exception e)
            {
                return BadRequest(e);
            }
        }
    }
}