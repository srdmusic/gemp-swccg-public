FROM public.ecr.aws/docker/library/mariadb:latest

COPY ./database_script.sql /docker-entrypoint-initdb.d
COPY ./initial_user_setup.sql /docker-entrypoint-initdb.d
