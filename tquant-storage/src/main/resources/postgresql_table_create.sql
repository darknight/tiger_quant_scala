CREATE DATABASE tiger_quant;

CREATE TABLE bar
(
    id          SERIAL PRIMARY KEY,
    symbol      varchar(20)    NOT NULL DEFAULT '',
    duration    bigint         NOT NULL DEFAULT '0',
    period      varchar(10)    NOT NULL DEFAULT '',
    open        numeric(15, 4) NOT NULL DEFAULT '0.0000',
    high        numeric(15, 4) NOT NULL DEFAULT '0.0000',
    low         numeric(15, 4) NOT NULL DEFAULT '0.0000',
    close       numeric(15, 4) NOT NULL DEFAULT '0.0000',
    volume      int            NOT NULL DEFAULT '0',
    amount      numeric(15, 4) NOT NULL DEFAULT '0.0000',
    time        timestamp      NOT NULL DEFAULT now(),
    create_time timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_create_time ON bar (create_time);

CREATE TABLE contract
(
    id             SERIAL PRIMARY KEY,
    identifier     varchar(40)      NOT NULL,
    name           varchar(50)      NOT NULL DEFAULT '',
    symbol         varchar(20)      NOT NULL DEFAULT '',
    sec_type       varchar(20)      NOT NULL DEFAULT '',
    currency       varchar(10)      NOT NULL DEFAULT '',
    exchange       varchar(10)      NOT NULL DEFAULT '',
    market         varchar(10)      NOT NULL DEFAULT '',
    expiry         varchar(10)      NOT NULL DEFAULT '',
    contract_month varchar(10)      NOT NULL DEFAULT '',
    strike         double precision NOT NULL DEFAULT '0.0000',
    multiplier     double precision NOT NULL DEFAULT '0.0000',
    "right"        varchar(10)      NOT NULL DEFAULT '',
    min_tick       double precision NOT NULL DEFAULT '0.0000',
    lot_size       int                       DEFAULT '0',
    create_time    timestamp        NOT NULL DEFAULT now(),
    update_time    timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uniq_idx_identifier ON contract (identifier);
CREATE INDEX idx_symbol ON contract (symbol);
CREATE INDEX idx_create_time ON contract (create_time);

CREATE TABLE tick
(
    id            BIGSERIAL PRIMARY KEY,
    seq_no        bigint           NOT NULL DEFAULT '0',
    symbol        varchar(20)      NOT NULL DEFAULT '0',
    volume        int              NOT NULL DEFAULT '0',
    amount        double precision          DEFAULT '0.0000',
    type          varchar(10)      NOT NULL DEFAULT '',
    latest_price  double precision NOT NULL DEFAULT '0',
    latest_volume double precision NOT NULL DEFAULT '0',
    latest_time   timestamp(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time          bigint           NOT NULL DEFAULT '0',
    bid_price     double precision NOT NULL DEFAULT '0',
    bid_size      bigint           NOT NULL DEFAULT '0',
    ask_price     double precision NOT NULL DEFAULT '0',
    ask_size      bigint           NOT NULL DEFAULT '0',
    open          double precision          DEFAULT '0.0000',
    high          double precision          DEFAULT '0.0000',
    low           double precision          DEFAULT '0.0000',
    close         double precision          DEFAULT '0.0000',
    create_time   timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_symbol_time ON tick (symbol);
