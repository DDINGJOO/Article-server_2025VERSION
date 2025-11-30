-- MariaDB 최대 연결 수 증가 스크립트
-- 현재: 151 -> 목표: 500

-- 1. 현재 설정 확인
SELECT @@max_connections as 'Current max_connections';

-- 2. 현재 연결 상태 확인
SHOW STATUS LIKE 'Threads_connected';

-- 3. 최대 연결 수를 500으로 증가 (즉시 적용)
SET GLOBAL max_connections = 500;

-- 4. 변경된 설정 확인
SELECT @@max_connections as 'New max_connections';

-- 5. 현재 연결 목록 확인 (어떤 서비스가 연결 중인지)
SELECT user,
       host,
       db,
       command,
       time,
       state
FROM information_schema.processlist
WHERE db = 'article_db'
ORDER BY time DESC;

-- 6. 연결 관련 통계 확인
SHOW STATUS LIKE 'Max_used_connections';
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Connection_errors%';
SHOW STATUS LIKE 'Aborted_connects';

-- 7. 추가 권장 설정 (선택사항)
-- SET GLOBAL max_allowed_packet = 67108864; -- 64MB
-- SET GLOBAL wait_timeout = 28800;
-- SET GLOBAL interactive_timeout = 28800;
