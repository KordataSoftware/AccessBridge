# AccessBridge

Makes MS Access databases available over a network through a REST API

## Server

Server is a .NET Core app that runs on Windows machines and exposes a simple REST API for querying and mutating MS Access databases on file systems accessible to that machine.

### Requirements

You need to have the [Microsoft Access Database Engine 2010 Redistributable](https://www.microsoft.com/en-us/download/details.aspx?id=13255) installed on the machine the Server will run on. You can use the linked installer or install with Chocolatey:

```sh
choco install msaccess2010-redist-x64
```

### Database Location

The location to look for databases needs to be set in the Server's `appsettings.json` file. Take a look in there for an example.

### API

There are currently two endpoints:

#### Query

```http
POST /v1/{database name}/query
```

Queries the named database. Here's an example request body:

```json
{
  "command": "SELECT * FROM Customers WHERE `Last Name` = ?",
  "parameters": {
    "Last Name": "Smith"
  }
}
```

Returns an array of JSON objects with the results:

```json
[
  {
    "First Name": "John",
    "Last Name": "Smith"
  },
  {
    "First Name": "Jane",
    "Last Name": "Smith"
  }
]
```

#### Mutate

```http
POST /v1/{database name}/mutate
```

Performs a mutation operation on the named database. Here's an example request body:

```json
{
  "command": "INSERT INTO CUSTOMERS (`First Name`, `Last Name`) VALUES (?,?)",
  "parameters": {
    "First Name": "Albert",
    "Last Name": "Smith"
  }
}
```

Returns a JSON object with the number of rows affected:

```json
{
  "rowsAffected": 1
}
```

### Auth

If the target Access Database requires a username / and password, the server will attempt to use the credentials in the `Authorization` header, if set.

## LoadTest

This hasn't been written yet, but the intent is to find out where this system breaks. Access isn't really supposed to be used like this, but there's value in making it available for scenarios like integration with other systems.
