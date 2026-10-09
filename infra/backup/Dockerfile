FROM alpine:3.23

# Cliente 18, igual ao Neon e ao Postgres local: o pg_dump não conecta em servidor mais novo que ele.
RUN apk add --no-cache postgresql18-client tzdata bash \
 && addgroup -S -g 10002 backup && adduser -S -u 10002 -G backup backup \
 && mkdir /backups && chown backup:backup /backups

COPY backup.sh restore.sh /usr/local/bin/
RUN sed -i 's/\r$//' /usr/local/bin/*.sh && chmod +x /usr/local/bin/backup.sh /usr/local/bin/restore.sh

USER backup
ENV TZ=America/Sao_Paulo
CMD ["/usr/local/bin/backup.sh", "loop"]
