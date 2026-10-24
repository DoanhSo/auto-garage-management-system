FROM nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker/runtime-config.js.template /opt/autogarage/runtime-config.js.template
COPY docker/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-runtime-config.sh
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=5s --retries=6 CMD wget -q -O /dev/null http://127.0.0.1/healthz || exit 1
CMD ["nginx", "-g", "daemon off;"]
