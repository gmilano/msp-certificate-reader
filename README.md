# msp-certificate-reader


Provide an implementation as a AWS Lambda Function to read a UY vaccination certificate.

The service receive the certificate in pdf, scan the QR inside it and return the information in json format.


# Configuration

In order to set AWS Credentials

aws config 


# Build

cd infraestructure
mvn package

# Deploy

cd infraestructure
cdk deploy

# Runtime

Be sure to set an environment variable "pubkey" in your Lambda AWS environment.

# Live Service that you can use

curl --request POST 'https://t0rzbqh2hh.execute-api.us-east-2.amazonaws.com/validate?pubkey=' \
--data-binary '@/Users/gmilano/Documents/Genexus/qrvacunas/certificadogaston.pdf'

This is going to return the information inside the QR Code 

```javascript
{
    "Date": "2021-08-12",
    "Name": "Gaston Milano",
    "CountryCode": "858",
    "DocumentType": "CI",
    "DocumentNumber": "34222313",
    "VaccinationInfo": {
        "Doses": [
            {
                "Number": 2,
                "Date": "2021-04-21",
                "Vaccine": "SINOVAC"
            },
            {
                "Number": 1,
                "Date": "2021-03-25",
                "Vaccine": "SINOVAC"
            }
        ]
    }
}
```


