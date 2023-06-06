# JSON-Database
Improve your database in this stage. It should be able to store not only strings but any JSON objects as values.

The key should not only be a string since the user needs to retrieve part of the JSON value. For example, in the code snippet below, the user wants to get only the surname of the person:

{
    ... ,

    "person": {
        "name": "Adam",
        "surname": "Smith"
    }
    ...
}
Then, the user should type the full path to this field in a form of a JSON array: ["person", "surname"]. If the user wants to get the full person object, then they should type ["person"]. The user should be able to set separate values inside JSON values. For example, it should be possible to set only the surname using a key ["person", "surname"] and any value including another JSON. Moreover, the user should be able to set new values inside other JSON values. For example, using a key ["person", "age"] and a value 25, the person object should look like this:

{
    ... ,

    "person": {
        "name": "Adam",
        "surname": "Smith",
        "age": 25

    }
    ...
}
If there are no root objects, the server should create them, too. For example, if the database does not have a "person1" key but the user set the value {"id1": 12, "id2": 14} for the key ["person1", "inside1", "inside2"], then the database will have the following structure:

{
    ... ,
    "person1": {
        "inside1": {
            "inside2" : {
                "id1": 12,
                "id2": 14
            }
        }
    },
    ...
}
The deletion of objects should follow the same rules. If a user deletes the object above by the key ["person1", "inside1", "inside2], then only "inside2" should be deleted, not "inside1" or "person1". See the example below:

{
    ... ,
    "person1": {
        "inside1": { }
    }

    ...
}
Objectives
Enhance your database with the ability to store any JSON objects as values as portrayed at the description.
Example
The greater-than symbol followed by a space (> ) represents the user input. Note that it's not part of the input.

Starting the server:

> java Main
Server started!
There is no need to format JSON in the output.

Starting the clients:

> java Main -t set -k 1 -v "Hello world!" 
Client started!
Sent: {"type":"set","key":"1","value":"Hello world!"}
Received: {"response":"OK"}
> java Main -in setFile.json 
Client started!
Sent:
{
   "type":"set",
   "key":"person",
   "value":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"87"
      }
   }
}
Received: {"response":"OK"}
> java Main -in getFile.json 
Client started!
Sent: {"type":"get","key":["person","name"]}
Received: {"response":"OK","value":"Elon Musk"}
> java Main -in updateFile.json 
Client started!
Sent: {"type":"set","key":["person","rocket","launches"],"value":"88"}
Received: {"response":"OK"}
> java Main -in secondGetFile.json 
Client started!
Sent: {"type":"get","key":["person"]}
Received:
{
   "response":"OK",
   "value":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster",
         "year":"2018"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"88"
      }
   }
}
> java Main -in deleteFile.json 
Client started!
Sent: {"type":"delete","key":["person","car","year"]}
Received: {"response":"OK"}
> java Main -in secondGetFile.json 
Client started!
Sent: {"type":"get","key":["person"]}
Received:
{
   "response":"OK",
   "value":{
      "name":"Elon Musk",
      "car":{
         "model":"Tesla Roadster"
      },
      "rocket":{
         "name":"Falcon 9",
         "launches":"88"
      }
   }
}
> java Main -t exit 
Client started!
Sent: {"type":"exit"}
Received: {"response":"OK"}
