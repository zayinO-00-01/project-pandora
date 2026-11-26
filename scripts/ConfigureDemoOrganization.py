#!/usr/bin/env python3
"""Opt-in setup for the Pandora demo. Adds missing accounts and names the four known departments."""
from argparse import ArgumentParser
from datetime import date
from getpass import getpass
import json
import os
from pathlib import Path
import re
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

DEPARTMENTS = {
    'leader': '综合管理部', 'dept_tech': '技术研发部', 'dept_market': '市场销售部', 'dept_ops': '运营服务部',
}
TEAMS = {
    'team_lead': ('综合组组长', 'leader', [('general_1', '周敏'), ('general_2', '郭晓')]),
    'lead_backend': ('后端组长', 'dept_tech', [('dev_1', '赵强'), ('dev_2', '陈晨'), ('dev_3', '周涛'), ('qa_1', '许宁')]),
    'lead_frontend': ('前端组长', 'dept_tech', [('fe_1', '林静'), ('fe_2', '何磊'), ('qa_2', '沈琪')]),
    'lead_sales': ('销售组长', 'dept_market', [('sales_1', '孙悦'), ('sales_2', '郑浩'), ('sales_3', '冯雪'), ('mkt_1', '梁洁')]),
    'lead_brand': ('品牌组长', 'dept_market', [('brand_1', '许晴'), ('brand_2', '邓超'), ('mkt_2', '孔琳')]),
    'lead_service': ('客服组长', 'dept_ops', [('cs_1', '曹阳'), ('cs_2', '彭丽'), ('cs_3', '唐磊'), ('ops_1', '罗媛'), ('ops_2', '唐欣')]),
}
HEADS = {'leader': '部门老总', 'dept_tech': '技术部老总', 'dept_market': '市场部老总', 'dept_ops': '运营部老总'}


def plan():
    rows = [(name, 'DEPT_HEAD', display, 'founder') for name, display in HEADS.items()]
    for lead, (display, dept, members) in TEAMS.items():
        rows.append((lead, 'TEAM_LEAD', display, dept))
        rows.extend((name, 'STAFF', name_label, lead) for name, name_label in members)
    return rows


def main():
    parser = ArgumentParser(description=__doc__)
    parser.add_argument('--server', default='http://127.0.0.1:8080')
    parser.add_argument('--apply', action='store_true', help='write changes; default is a read-only preview')
    parser.add_argument('--identities', type=Path, help='local JSON mapping new usernames to their 18-character identity numbers')
    args = parser.parse_args()
    server = urlsplit(args.server)
    if not server.hostname or server.username or server.password or server.query or server.fragment or server.path not in ('', '/'):
        parser.error('Please provide a server origin.')
    if server.scheme != 'https' and (server.scheme != 'http' or server.hostname not in ('127.0.0.1', 'localhost', '::1')):
        parser.error('Use localhost, an SSH tunnel, or HTTPS when sending passwords.')
    base = args.server.rstrip('/') + '/api/v1/'

    def api(path, data=None, method=None, token=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        try:
            req = Request(base + path, data=json.dumps(data).encode() if data is not None else None, method=method, headers=headers)
            with urlopen(req, timeout=12) as response:
                return json.load(response)
        except HTTPError as error:
            raise SystemExit(f'{path}: HTTP {error.code}') from None

    admin_password = os.environ.get('PANDORA_ORG_ADMIN_PASSWORD') or getpass('Administrator password: ')
    token = api('auth/login', {'username': 'admin', 'password': admin_password})['token']
    users = {u['username']: u for u in api('admin/users', token=token)}
    if not users.get('founder') or users['founder']['disabled'] or users['founder']['role'] != 'FOUNDER':
        raise SystemExit('An active founder demo account is required.')
    pending = plan()
    for username, role, _, parent in pending:
        account = users.get(username)
        if account is not None and (account['disabled'] or account['role'] != role or account['managerId'] != users.get(parent, {}).get('id')):
            raise SystemExit(f'Existing reporting relationship differs for {username}; review it before proceeding.')
        if account is not None and 'departmentName' not in account:
            raise SystemExit('Deploy the backend with department naming support first.')
    new = [name for name, _, _, _ in pending if name not in users]
    rename = {name: label for name, label in DEPARTMENTS.items() if name in users and not users[name]['departmentName']}
    print('New accounts:', ', '.join(new) or 'none')
    print('Departments to name:', ', '.join(rename.values()) or 'none')
    if not args.apply:
        return
    identities = {}
    if new:
        if not args.identities:
            parser.error('--identities is required when creating accounts.')
        try:
            supplied = json.loads(args.identities.read_text(encoding='utf-8-sig'))
        except (OSError, ValueError):
            parser.error('Unable to read the identities JSON file.')
        if not isinstance(supplied, dict):
            parser.error('Identities must be a JSON object keyed by username.')
        used_ids = {u['idCard'].upper() for u in users.values() if u.get('idCard')}
        for username in new:
            raw = supplied.get(username)
            card = raw.strip().upper() if isinstance(raw, str) else ''
            if not re.fullmatch(r'[0-9]{17}[0-9X]', card) or card.startswith('000000'):
                parser.error(f'Invalid or missing 18-character identity number for {username}.')
            weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
            if card[-1] != '10X98765432'[sum(int(digit) * w for digit, w in zip(card, weights)) % 11]:
                parser.error(f'Invalid identity checksum for {username}.')
            try:
                birthday = date(int(card[6:10]), int(card[10:12]), int(card[12:14]))
            except ValueError:
                parser.error(f'Invalid identity birth date for {username}.')
            if birthday > date.today() or card in used_ids:
                parser.error(f'Duplicate identity number or future birth date for {username}.')
            used_ids.add(card)
            identities[username] = card
    new_password = (os.environ.get('PANDORA_ORG_DEMO_PASSWORD') or getpass('Password for new demo accounts: ')) if new else None
    if new and not 6 <= len(new_password.encode('utf-8')) <= 72:
        raise SystemExit('Demo password must have 6–72 UTF-8 bytes.')
    used = {user['phone'] for user in users.values() if user.get('phone')}
    counter = 3301
    for username, role, display, parent in pending:
        if username in users:
            continue
        while f'199{counter:08d}' in used:
            counter += 1
        phone = f'199{counter:08d}'
        used.add(phone)
        account = {'username': username, 'password': new_password, 'role': role, 'displayName': display, 'phone': phone,
                   'managerId': users[parent]['id'], 'idCard': identities[username]}
        if role == 'DEPT_HEAD':
            account['departmentName'] = DEPARTMENTS[username]
        users[username] = api('admin/users', account, token=token)
    for username, label in rename.items():
        saved = api(f'admin/users/{users[username]["id"]}', {'departmentName': label}, 'PATCH', token)
        if saved['departmentName'] != label:
            raise SystemExit(f'Department name for {username} did not save.')
    print(f'Added {len(new)} account(s), named {len(rename)} existing department(s).')


if __name__ == '__main__':
    main()
