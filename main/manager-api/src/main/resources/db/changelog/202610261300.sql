-- Agent templates: drop "Taiwanese Girl Xiaohe" and make Malek the first (default) template
delete from `ai_agent_template` where id = '9406648b5cc5fde1b8aa335b6f8b4f76';

update `ai_agent_template` set `sort` = 1 where id = 'f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6';
update `ai_agent_template` set `sort` = 2 where id = '0ca32eb728c949e58b1000b2e401f90c';
update `ai_agent_template` set `sort` = 3 where id = '6c7d8e9f0a1b2c3d4e5f6a7b8c9d0s24';
update `ai_agent_template` set `sort` = 4 where id = 'e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b1';
update `ai_agent_template` set `sort` = 5 where id = 'a45b6c7d8e9f0a1b2c3d4e5f6a7b8c92';
