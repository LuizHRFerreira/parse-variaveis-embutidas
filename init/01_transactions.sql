-- Roda só na primeira inicialização do banco
ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE TABLE aula.transactions (
  txid   NUMBER PRIMARY KEY,
  amount NUMBER(12,2)
);

-- txid de 1000 a 101000, igual ao laço do teste dos slides
INSERT INTO aula.transactions (txid, amount)
SELECT 999 + LEVEL, ROUND(DBMS_RANDOM.VALUE(1, 10000), 2)
FROM dual CONNECT BY LEVEL <= 100001;

COMMIT;

-- Permite ao usuário ler as estatísticas de parse (v$sysstat, v$mystat, v$sql)
GRANT SELECT_CATALOG_ROLE TO aula;
GRANT SELECT ON v_$mystat   TO aula;
GRANT SELECT ON v_$statname TO aula;
GRANT SELECT ON v_$sysstat  TO aula;
GRANT SELECT ON v_$sql      TO aula;

-- Permite limpar o cache de SQL (shared pool) entre as rodadas do teste
GRANT ALTER SYSTEM TO aula;
