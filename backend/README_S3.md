# S3 setup (presigned GET URLs)

This backend generates **presigned GET URLs** for license plate images.

## 1) Required env vars

Set the following:

- `S3_BUCKET_NAME` – your bucket name
- `S3_REGION` – AWS region (e.g. `ap-southeast-1`)
- `S3_PRESIGN_EXPIRE_SECONDS` – optional, default `300`

AWS credentials are read from the default AWS SDK provider chain. For local/Docker, the easiest is:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN` (only if you use temporary credentials)

## 2) IAM permissions

The credentials used by the backend need at least:

- `s3:GetObject` on `arn:aws:s3:::<bucket>/*`

## 3) API

Frontend calls:

`POST /api/s3/presign-get`

Body:
```json
{ "key": "plates/ps-1/log-10.jpg" }
```

Response:
```json
{ "url": "https://..." }
```

## 4) Data requirements

Entry logs must have `license_plate_image_key` filled, otherwise the UI shows `—`.

