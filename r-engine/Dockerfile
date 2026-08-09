FROM rocker/r-ver:4.4.1

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    libcurl4-openssl-dev \
    libssl-dev \
    libsodium-dev \
    libxml2-dev \
    libfontconfig1-dev \
    libharfbuzz-dev \
    libfribidi-dev \
    libfreetype6-dev \
    libpng-dev \
    libtiff5-dev \
    libjpeg-dev \
    pkg-config \
    ca-certificates \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN Rscript -e "options(repos = c(CRAN = 'https://cloud.r-project.org')); pkgs <- c('plumber','jsonlite'); install.packages(pkgs, dependencies = c('Depends','Imports','LinkingTo')); missing <- pkgs[!vapply(pkgs, requireNamespace, logical(1), quietly = TRUE)]; if (length(missing) > 0) { install.packages(missing, dependencies = c('Depends','Imports','LinkingTo')); }; still_missing <- pkgs[!vapply(pkgs, requireNamespace, logical(1), quietly = TRUE)]; if (length(still_missing) > 0) { stop(paste('Missing required R packages:', paste(still_missing, collapse = ', '))); }"

WORKDIR /opt/r-engine

COPY plumber.R /opt/r-engine/plumber.R

RUN mkdir -p /work/input /work/output /work/tmp

EXPOSE 8000

CMD ["R", "-q", "-e", "plumber::pr('/opt/r-engine/plumber.R')$run(host='0.0.0.0', port=8000)"]
