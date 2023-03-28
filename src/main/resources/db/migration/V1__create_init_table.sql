create extension if not exists "uuid-ossp";

create type access_modifier as enum ('PRIVATE', 'PUBLIC');
create type user_role as enum ('USER', 'CAMPAIGN_ADMIN', 'ADMIN');

create table time_zone
(
    id         bigserial primary key,
    country    varchar(64)                            not null,
    zone_id    varchar(64)                            not null,
    utc        varchar(16)                            not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    deleted_at timestamp with time zone
);

insert into time_zone(country, zone_id, utc)
values ('South Korea', 'Asia/Seoul', '+09:00');
insert into time_zone(country, zone_id, utc)
values ('India', 'Asia/Kolkata', '+05:30');

create table position
(
    id         bigserial primary key,
    name       varchar(64)                            not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    deleted_at timestamp with time zone
);

create table "group"
(
    id         bigserial primary key,
    name       varchar(64)                            not null,
    parent_id  bigint references "group" (id),
    level      int                                    not null,
    pathways   bigint[]                 default '{}'  not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    deleted_at timestamp with time zone
);

create view group_view as
with recursive group_record(id, parent_id, name, level, path, cycle) as (
    select g.id,
           g.parent_id,
           g.name,
           1,
           array [g.id],
           false
    from "group" g
    where g.parent_id is null
      and g.deleted_at is null
    union all
    select g.id,
           g.parent_id,
           g.name,
           gr.level + 1,
           path || g.id,
           g.id = any (path)
    from "group" g
             join group_record gr on g.parent_id = gr.id
    where not cycle
      and g.deleted_at is null
)
select id, parent_id, name, level, path as pathways
from group_record;

create table "user"
(
    id           bigserial primary key,
    name         varchar(64)                             not null,
    username     varchar(256) unique                     not null,
    password     varchar(256)                            not null,
    email        varchar(64) unique                      not null,
    role         user_role                default 'USER' not null,
    time_zone_id bigint references time_zone (id)        not null,
    active       boolean                  default false  not null,
    created_at   timestamp with time zone default now()  not null,
    updated_at   timestamp with time zone default now()  not null,
    deleted_at   timestamp with time zone
);

insert into "user"(name, username, email, role, time_zone_id, active, password)
values ('Admin', 'admin', 'admin@pharmcadd.com', 'ADMIN', 1, true,
        '{bcrypt}$2a$10$FpSZuzzhRJEssDbd6zkgTuV8tc.kmHZS9D4FCSbG1uGB0ea1kE4OK');

insert into "user"(name, username, email, role, time_zone_id, active, password)
values ('Guest', 'guest', 'guest@pharmcadd.com', 'USER', 1, true,
        '{bcrypt}$2a$10$FpSZuzzhRJEssDbd6zkgTuV8tc.kmHZS9D4FCSbG1uGB0ea1kE4OK');

-- uri 에는 storage 가 local 인경우에는 file:// 로 시작하도록 한다.
-- 외부 서비스인 경우에는 http://, 또는 https:// 로 시작
create table "attachment"
(
    id         bigserial primary key,
    key        uuid                     default uuid_generate_v4() not null,
    name       varchar(256)                                        not null,
    size       bigint                   default 0                  not null,
    uri        varchar(512)                                        not null,
    created_by bigint references "user" (id)                       not null,
    created_at timestamp with time zone default now()              not null,
    updated_at timestamp with time zone default now()              not null,
    deleted_at timestamp with time zone
);

create table authorization_code
(
    id         bigserial primary key,
    email      varchar(64)                                         not null,
    code       varchar(6)                                          not null,
    is_verify  boolean                  default false              not null,
    expired_at timestamp with time zone default now() + '5 minute' not null,
    created_at timestamp with time zone default now()              not null,
    updated_at timestamp with time zone default now()              not null,
    deleted_at timestamp with time zone
);

create table group_user
(
    id          bigserial primary key,
    group_id    bigint references "group" (id)         not null,
    user_id     bigint references "user" (id)          not null,
    position_id bigint references "position" (id),
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);

-- 진행 중인 캠페인이 존재한다면 질문 및 문항은 수정 되어서는 안된다.
create table form
(
    id          bigserial primary key,
    title       varchar(256)                           not null,
    description varchar(4000),
--     anonymous   boolean                  default false not null,
    created_by  bigint references "user" (id)          not null,
    updated_by  bigint references "user" (id)          not null,
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);

create type form_schedule_type as enum ('MANUAL', 'CRON');

-- duration 의 단위는 밀리세컨으로 인지
-- type 이 MANUAL 일 때는 starts_at 는 필수
-- type 이 CRON 일 때는 cron_expression, cron_time_zone_id 필수
create table form_schedule
(
    id              bigserial primary key,
    form_id         bigint references form (id)            not null,
    type            form_schedule_type                     not null,
    time_zone_id    bigint references time_zone (id)       not null,
    starts_at       timestamp without time zone,
    ends_at         timestamp without time zone,
    cron_expression varchar(64),
    cron_duration   bigint,
    active          boolean                  default false not null,
    created_at      timestamp with time zone default now() not null,
    updated_at      timestamp with time zone default now() not null,
    deleted_at      timestamp with time zone
);

create type participant_type as enum ('USER', 'GROUP');

-- 폼의 스케쥴링 시 사용 시 참여자/그룹
-- form 테이블에 user_ids, group_ids bigint[] array 로 선언하지 않는 이유는
-- array 는 table references 를 제약 할 수 없기 때문이다.
create table form_schedule_participant
(
    id               bigserial primary key,
    schedule_id      bigint references form_schedule (id)   not null,
    form_id          bigint references form (id)            not null,
    type             participant_type                       not null,
    user_id          bigint references "user" (id),
    group_id         bigint references "group" (id),
    include_subgroup boolean                  default false not null,
    created_at       timestamp with time zone default now() not null,
    updated_at       timestamp with time zone default now() not null,
    deleted_at       timestamp with time zone
);

-- 캠페인 시간 만료나, 정상 완료 시 노티 받을 사람
create table form_notification
(
    id         bigserial primary key,
    form_id    bigint references form (id)            not null,
    user_id    bigint references "user" (id)          not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    deleted_at timestamp with time zone
);

-- TEXT_SHORT 과 TEXT_LONG 는 UI 상의 차이 일 뿐 DB 에는 똑같은 varchar(1000) 에 저장 한다.
create type question_type as enum (
    'CHOICE_SINGLE',
    'CHOICE_MULTIPLE',
    'TEXT_SHORT',
    'TEXT_LONG',
    'DATE',
    'DATE_TIME',
    'ATTACHMENT',
    'USER',
    'GROUP'
    );

create table question
(
    id         bigserial primary key,
    form_id    bigint references form (id)            not null,
    title      varchar(256)                           not null,
    abbr       varchar(256),
    type       question_type                          not null,
    required   boolean                  default false not null,
    "order"    int                                    not null,
    created_at timestamp with time zone default now() not null,
    updated_at timestamp with time zone default now() not null,
    deleted_at timestamp with time zone
);

create table question_option
(
    id          bigserial primary key,
    form_id     bigint references form (id)            not null,
    question_id bigint references question (id)        not null,
    text        varchar(256)                           not null,
    "order"     int                                    not null,
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);

create type campaign_status as enum ('READY', 'RUNNING', 'SUSPENDED', 'FINISHED', 'STOPPED');

-- 시작시간, 종료시간을 입력하지 않았다면 관리자가 매뉴얼로 status 관리를 해야 한다.
-- 익명 처리는 설문한 사용자 의 hidden 이 아니라 설문 응답 내용에 대한 hidden 처리

-- TODO : 스케쥴러가 동작 시킨 캠페인지 아닌지 구분이 필요 하다.
create table campaign
(
    id              bigserial primary key,
    form_id         bigint references form (id)               not null,
    title           varchar(256)                              not null,
    description     varchar(4000),
--     owner           bigint references "user" (id),
    starts_at       timestamp with time zone,
    ends_at         timestamp with time zone,
    access_modifier access_modifier          default 'PUBLIC' not null,
    status          campaign_status          default 'READY'  not null,
--     anonymous       boolean                  default false    not null,
    created_by      bigint references "user" (id)             not null,
    updated_by      bigint references "user" (id)             not null,
    created_at      timestamp with time zone default now()    not null,
    updated_at      timestamp with time zone default now()    not null,
    deleted_at      timestamp with time zone
);

-- campaign 의 access_modifier 가 PRIVATE 인 경우에, 특정 유저 & 특정 부서만 참여 가능
-- 여기서 group 은 tree 가 아닌 flat 하다고 보자.
create table participant
(
    id          bigserial primary key,
    campaign_id bigint references campaign (id)        not null,
    type        participant_type                       not null,
    user_id     bigint references "user" (id),
    group_id    bigint references "group" (id),
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);

create index participant_campaign_id_type_user_id_index
    on participant (campaign_id, type, user_id);

create index participant_campaign_id_type_group_id_index
    on participant (campaign_id, type, group_id);

-- 캠페인 참여자(only user) 테이블
create view participant_user_view as
(
select p.campaign_id,
       u.id as user_id
from participant p
         join "user" u on p.user_id = u.id and p.type = 'USER' and u.deleted_at is null
where p.deleted_at is null
union
select p.campaign_id,
       u.id as user_id
from participant p
         join "group" g on p.group_id = g.id and p.type = 'GROUP' and g.deleted_at is null
         join group_user gu on g.id = gu.group_id and gu.deleted_at is null
         join "user" u on gu.user_id = u.id and u.deleted_at is null
where p.deleted_at is null
    );

-- 응답한 사람만 저장 처리
-- 익명 설문을 해야 한다면 respondent 는 실제 유저 아이디를 저장하고
-- answer 의 created_by 에는 anonymous id 를 저장 처리
create table respondent
(
    id          bigserial primary key,
    campaign_id bigint references campaign (id)        not null,
    user_id     bigint references "user" (id)          not null,
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);

-- text 는 반드시 주관식만 입력할 필요는 없다.
-- 객관식이라 하더라도 question_option_id 로 question_option 의 text 값을 그대로 저장처리 하도록 한다.

-- postgresql 의 varchar(1000) 은 1000 문자를 뜻한다. 실제 byte 는 1000 * 4 = 4000 로 잡힌다.
-- 쇼핑몰 후기나, 배민 후기를 생각하면 1000 자는 생각보다 길다.
-- question 의 type 이 attachment 의 경우 text 에 파일명 저장
create table answer
(
    id            bigserial primary key,

    form_id       bigint references form (id)            not null,
    campaign_id   bigint references campaign (id)        not null,
    question_id   bigint references question (id)        not null,
    option_id     bigint references question_option (id),
    attachment_id bigint references attachment (id),
    user_id       bigint references "user" (id),
    group_id      bigint references "group" (id),
    text          varchar(1000)                          not null,
    created_by    bigint references "user" (id)          not null,
    created_at    timestamp with time zone default now() not null,
    updated_at    timestamp with time zone default now() not null,
    deleted_at    timestamp with time zone
);

create type approval_type as enum ('REQUEST', 'APPROVE', 'REJECT');

-- 설문 취소 요청 프로세스
create table cancel_answer
(
    id            bigserial primary key,
    campaign_id   bigint references campaign (id)            not null,
    approval_type approval_type            default 'REQUEST' not null,
    requester     bigint references "user" (id)              not null,
    reason        varchar(1000)                              not null,
    approver      bigint references "user" (id),
    answer        varchar(1000),
    created_at    timestamp with time zone default now()     not null,
    updated_at    timestamp with time zone default now()     not null,
    deleted_at    timestamp with time zone
);

-- 사용자가 응답할 때마다 질문 순서대로 저장처리 한다.
-- varchar(1000) 주르륵은 낭비인가? 낭비 맞다.
create table answer_stat
(
    id          bigserial primary key,
    campaign_id bigint references campaign (id)        not null,
    user_id     bigint references "user" (id)          not null,
    ans_1       varchar(1000),
    ans_2       varchar(1000),
    ans_3       varchar(1000),
    ans_4       varchar(1000),
    ans_5       varchar(1000),
    ans_6       varchar(1000),
    ans_7       varchar(1000),
    ans_8       varchar(1000),
    ans_9       varchar(1000),
    ans_10      varchar(1000),
    ans_11      varchar(1000),
    ans_12      varchar(1000),
    ans_13      varchar(1000),
    ans_14      varchar(1000),
    ans_15      varchar(1000),
    ans_16      varchar(1000),
    ans_17      varchar(1000),
    ans_18      varchar(1000),
    ans_19      varchar(1000),
    ans_20      varchar(1000),
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null,
    deleted_at  timestamp with time zone
);
