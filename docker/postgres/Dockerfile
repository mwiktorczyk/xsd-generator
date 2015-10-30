FROM postgres:latest
ENV POSTGRES_CUSTOM_DATABASE=dbunit1 POSTGRES_CUSTOM_USER=dbuser POSTGRES_CUSTOM_PASSWORD=dbpass POSTGRES_PASSWORD=mysecretpassword
COPY creade_db.sh /docker-entrypoint-initdb.d/
