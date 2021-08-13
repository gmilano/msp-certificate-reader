# msp-certificate-reader

The aim of this repository is provide a code that can be reused or improved to read UY vaccination certificate and on the other hand provide a Serverless architecture that host an instance of this service, so that if you don't mind to use a AWS hosted service for reading certificates you can use the one I hosted in my account.

The infrastructure is provisioned using AWS CDK so you can see the code of the infra, and deploy it on your own AWS cloud.

The service receive the certificate in pdf, scan the QR inside it and return the information in json format.

If you have an Online Ticketing or similar system and do you want to read a UY vaccination certificate you can use this service. 

Feel free to deploy this service in your own infrastructure or use the provided infrastructure. 

# I just want to use your hosted service

The entry point for the API [https://api.msp-uy.com/validate](https://api.msp-uy.com/validate)

## Commandline

Send a PDF
```
curl --request POST 'https://api.msp-uy.com/validate' \
--data-binary '@/Users/gmilano/Documents/Genexus/qrvacunas/certificadogaston.pdf'
```

Send the Content in Base45 read from a QR Code

```
curl --location --request POST 'https://api.msp-uy.com/validatee' \
--header 'Content-Type: text/plain' \
--data-raw '<base45 string, usually read from some QR Scanner>'
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

fetch("https://api.msp-uy.com/validate", requestOptions)
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
  .url("https://api.msp-uy.com/validate")
  .method("POST", body)
  .addHeader("Content-Type", "application/pdf")
  .build();
Response response = client.newCall(request).execute();
```

## Output 

When success the Http Code is 200 and the return body is the information inside the QR Code


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

# I want to deploy this service in my own AWS Account

# Recommendations

Install at least

- Java 11 & JDK
- VS Code
- AWS Toolkit
- Maven
- Docker

This project was developed in a Mac OSX so I don't know if this is working in a Windows box.

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

Be sure to set an environment variable "pubkey" in your Lambda AWS environment. [Set Env var in AWS Lambda](https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html)

The public key is provided by AGESIC.


# Scanner Sample

I create a simple Scanner sample in GeneXus using this service. I created a very simple Android and iOS native application.

If you want to test [Validator App](https://apps5.genexus.com/msp-utils/developermenu.html?qrcode)






