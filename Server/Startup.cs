using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using Microsoft.AspNetCore.Mvc;

namespace Kordata.AccessBridge.Server
{
    public class Startup
    {
        private readonly IConfiguration configuration;
        private readonly IHostingEnvironment environment;

        public Startup(IConfiguration configuration, IHostingEnvironment environment)
        {
            this.configuration = configuration;
            this.environment = environment;

            JsonConvert.DefaultSettings = () => SchemaSerializer.Settings;
        }

        // This method gets called by the runtime. Use this method to add services to the container.
        // For more information on how to configure your application, visit https://go.microsoft.com/fwlink/?LinkID=398940
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddMvc().SetCompatibilityVersion(CompatibilityVersion.Version_2_2);

            services.AddHttpClient("WatchWebhooks");

            services.AddSingleton<ICouchbaseLiteFactory, CouchbaseLiteFactory>(_ =>
                new CouchbaseLiteFactory(configuration.GetSection("WatchDatabase")));
            services.AddSingleton<IAccessConnectionFactory, AccessConnectionFactory>(_ =>
                new AccessConnectionFactory(configuration.GetSection("MicrosoftAccess")));

            services.AddTransient<IFileRepository, FileRepository>(_ =>
                new FileRepository(configuration.GetSection("FileUpload")));
            services.AddTransient<IWatchRepository, WatchRepository>();

            services.AddHostedService<TableWatcher>();
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseMvc();
        }
    }
}