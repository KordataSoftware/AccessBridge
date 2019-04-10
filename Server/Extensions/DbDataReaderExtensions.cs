using System.Data.Common;
using System.Linq;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Data;
using System.Data.SqlClient;

namespace Kordata.AccessBridge.Server
{
    public static class DbDataReaderExtensions
    {
        public static JObject GetJObject(this DbDataReader reader)
        {
            var properties = Enumerable.Range(0, reader.FieldCount)
                .Select(i => new JProperty(reader.GetName(i), reader.GetValue(i)));

            return new JObject(properties);
        }

        public static JArray GetSchemaJArray(this DbDataReader reader)
        {
            var columns = reader.GetCustomColumnSchema()
                .Select(s => JObject.FromObject(s));

            return new JArray(columns);
        }

        /// <summary>
        /// Custom column schema to support lack of native method. From https://github.com/dotnet/corefx/issues/26302#issuecomment-424609013
        /// </summary>
        private static ReadOnlyCollection<DbColumn> GetCustomColumnSchema(this DbDataReader reader)
        {
            IList<DbColumn> columnSchema = new List<DbColumn>();
            DataTable schemaTable = reader.GetSchemaTable();

            // ReSharper disable once PossibleNullReferenceException
            DataColumnCollection schemaTableColumns = schemaTable.Columns;
            foreach (DataRow row in schemaTable.Rows)
            {
                DbColumn dbColumn = new DataRowDbColumn(row, schemaTableColumns);
                columnSchema.Add(dbColumn);
            }

            ReadOnlyCollection<DbColumn> readOnlyColumnSchema = new ReadOnlyCollection<DbColumn>(columnSchema);
            return readOnlyColumnSchema;
        }

        /// From https://github.com/dotnet/corefx/issues/26302#issuecomment-424609013
        private class DataRowDbColumn : DbColumn
        {
            #region Fields
           
            private readonly DataColumnCollection schemaColumns;       
            private readonly DataRow schemaRow;

            #endregion

            #region Constructors and Destructors
            
            public DataRowDbColumn(DataRow readerSchemaRow, DataColumnCollection readerSchemaColumns)
            {
                this.schemaRow = readerSchemaRow;
                this.schemaColumns = readerSchemaColumns;
                this.PopulateFields();
            }

            #endregion

            #region Methods

            private T GetDbColumnValue<T>(string columnName)
            {
                if (!this.schemaColumns.Contains(columnName))
                {
                    return default(T);
                }

                object schemaObject = this.schemaRow[columnName];
                if (schemaObject is T variable)
                {
                    return variable;
                }

                return default(T);
            }

            /// <summary>
            /// </summary>
            private void PopulateFields()
            {
                this.AllowDBNull = this.GetDbColumnValue<bool?>(SchemaTableColumn.AllowDBNull);
                this.BaseCatalogName = this.GetDbColumnValue<string>(SchemaTableOptionalColumn.BaseCatalogName);
                this.BaseColumnName = this.GetDbColumnValue<string>(SchemaTableColumn.BaseColumnName);
                this.BaseSchemaName = this.GetDbColumnValue<string>(SchemaTableColumn.BaseSchemaName);
                this.BaseServerName = this.GetDbColumnValue<string>(SchemaTableOptionalColumn.BaseServerName);
                this.BaseTableName = this.GetDbColumnValue<string>(SchemaTableColumn.BaseTableName);
                this.ColumnName = this.GetDbColumnValue<string>(SchemaTableColumn.ColumnName);
                this.ColumnOrdinal = this.GetDbColumnValue<int?>(SchemaTableColumn.ColumnOrdinal);
                this.ColumnSize = this.GetDbColumnValue<int?>(SchemaTableColumn.ColumnSize);
                this.IsAliased = this.GetDbColumnValue<bool?>(SchemaTableColumn.IsAliased);
                this.IsAutoIncrement = this.GetDbColumnValue<bool?>(SchemaTableOptionalColumn.IsAutoIncrement);
                this.IsExpression = this.GetDbColumnValue<bool>(SchemaTableColumn.IsExpression);
                this.IsHidden = this.GetDbColumnValue<bool?>(SchemaTableOptionalColumn.IsHidden);
                this.IsIdentity = this.GetDbColumnValue<bool?>("IsIdentity");
                this.IsKey = this.GetDbColumnValue<bool?>(SchemaTableColumn.IsKey);
                this.IsLong = this.GetDbColumnValue<bool?>(SchemaTableColumn.IsLong);
                this.IsReadOnly = this.GetDbColumnValue<bool?>(SchemaTableOptionalColumn.IsReadOnly);
                this.IsUnique = this.GetDbColumnValue<bool?>(SchemaTableColumn.IsUnique);
                this.NumericPrecision = this.GetDbColumnValue<Int16?>(SchemaTableColumn.NumericPrecision);
                this.NumericScale = this.GetDbColumnValue<Int16?>(SchemaTableColumn.NumericScale);
                this.UdtAssemblyQualifiedName = this.GetDbColumnValue<string>("UdtAssemblyQualifiedName");
                this.DataType = this.GetDbColumnValue<Type>(SchemaTableColumn.DataType);
                this.DataTypeName = this.GetDbColumnValue<string>("DataTypeName");
            }

            #endregion
        }
    }
}