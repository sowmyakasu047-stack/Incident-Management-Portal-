import { describe, expect, it } from 'vitest';
import { canChangeStatus, canEdit, countBy, getErrorMessage, nextStatus } from './incidentUtils';

const admin = { userId: 1, role: 'ADMIN' };
const alice = { userId: 2, role: 'USER' };
const bob = { userId: 3, role: 'USER' };

describe('nextStatus', () => {
  it('follows the lifecycle', () => {
    expect(nextStatus('OPEN')).toBe('IN_PROGRESS');
    expect(nextStatus('IN_PROGRESS')).toBe('RESOLVED');
    expect(nextStatus('RESOLVED')).toBe('CLOSED');
  });
  it('has nothing after CLOSED or for unknown values', () => {
    expect(nextStatus('CLOSED')).toBeNull();
    expect(nextStatus('???')).toBeNull();
  });
});

describe('canChangeStatus', () => {
  const incident = { status: 'OPEN', createdBy: 2, assignedTo: 3 };
  it('allows admin and assignee', () => {
    expect(canChangeStatus(incident, admin)).toBe(true);
    expect(canChangeStatus(incident, bob)).toBe(true);
  });
  it('lets the creator only close a resolved incident', () => {
    expect(canChangeStatus(incident, alice)).toBe(false);
    expect(canChangeStatus({ ...incident, status: 'RESOLVED' }, alice)).toBe(true);
  });
  it('blocks everyone on closed incidents', () => {
    expect(canChangeStatus({ ...incident, status: 'CLOSED' }, admin)).toBe(false);
  });
});

describe('canEdit', () => {
  const incident = { status: 'OPEN', createdBy: 2, assignedTo: null };
  it('allows admin and creator but not strangers', () => {
    expect(canEdit(incident, admin)).toBe(true);
    expect(canEdit(incident, alice)).toBe(true);
    expect(canEdit(incident, bob)).toBe(false);
  });
  it('is false for closed incidents', () => {
    expect(canEdit({ ...incident, status: 'CLOSED' }, admin)).toBe(false);
  });
});

describe('getErrorMessage', () => {
  it('prefers field errors, then message, then fallback', () => {
    expect(getErrorMessage({ response: { data: { fieldErrors: { a: 'A bad', b: 'B bad' } } } })).toBe('A bad • B bad');
    expect(getErrorMessage({ response: { data: { message: 'Nope' } } })).toBe('Nope');
    expect(getErrorMessage({}, 'fallback')).toBe('fallback');
  });
});

describe('countBy', () => {
  it('counts items per key', () => {
    expect(countBy([{ s: 'A' }, { s: 'B' }, { s: 'A' }], 's')).toEqual({ A: 2, B: 1 });
  });
});
