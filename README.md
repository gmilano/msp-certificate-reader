# msp-certificate-reader

Provide an implementation as a AWS Lambda Function to read a UY vaccination certificate.


The service receive the certificate in pdf, scan the QR inside it and return the information in json format.

If you are an Online Ticketing system and do you want to validate a given certificate you can use this service. 

Feel free to deploy this service in your own infrastructure or use the provided infrastructure. 


# Configuration

In order to set AWS Credentials


aws config 


# Build

```
cd infraestructure
mvn package
```

# Deploy

```
cd infraestructure
cdk deploy
```

# Runtime

Be sure to set an environment variable "pubkey" in your Lambda AWS environment. 
The public key is provided by AGESIC.

# Live Service that you can use to send PDFs and read the QR Data programatically

## Commandline
```
curl --request POST 'https://t0rzbqh2hh.execute-api.us-east-2.amazonaws.com/validate' \
--data-binary '@/Users/gmilano/Documents/Genexus/qrvacunas/certificadogaston.pdf'
```

## Javascript
```javascript
var myHeaders = new Headers();
myHeaders.append("Content-Type", "application/pdf");

var file = "<file contents here>";

var requestOptions = {
  method: 'POST',
  headers: myHeaders,
  body: file,
  redirect: 'follow'
};

fetch("https://t0rzbqh2hh.execute-api.us-east-2.amazonaws.com/validate", requestOptions)
  .then(response => response.text())
  .then(result => console.log(result))
  .catch(error => console.log('error', error));
 ```
 
 ## Java
 ```java
 OkHttpClient client = new OkHttpClient().newBuilder()
  .build();
MediaType mediaType = MediaType.parse("application/pdf");
RequestBody body = RequestBody.create(mediaType, "<file contents here>");
Request request = new Request.Builder()
  .url("https://t0rzbqh2hh.execute-api.us-east-2.amazonaws.com/validate")
  .method("POST", body)
  .addHeader("Content-Type", "application/pdf")
  .build();
Response response = client.newCall(request).execute();
```

## Output 

```javascript
{
    "Date": "2021-08-12",
    "Name": "Gaston Milano",
    "CountryCode": "858",
    "DocumentType": "CI",
    "DocumentNumber": "34222113",
    "VaccinationInfo": {
        "Doses": [
            {
                "Number": 2,
                "Date": "2021-05-21",
                "Vaccine": "SINOVAC"
            },
            {
                "Number": 1,
                "Date": "2021-04-25",
                "Vaccine": "SINOVAC"
            }
        ]
    }
}
```


