alter table form_schedule
    add column mailing boolean default true;

alter table authorization_code
    rename column is_verify to verification;

alter table cancel_answer
    rename column requester to user_id;

alter table cancel_answer
    rename column approver to approved_by;

create type cancel_answer_status as enum ('REQUEST', 'APPROVE', 'REJECT');

alter table cancel_answer
    drop approval_type;

alter table cancel_answer
    add column status cancel_answer_status default 'REQUEST' not null;