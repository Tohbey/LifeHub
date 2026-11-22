import { parseNotificationEvent } from './notification-stream.service';

describe('parseNotificationEvent', () => {
  it('reads a notification event', () => {
    const n = parseNotificationEvent('event:notification\nid:n-1\ndata:{"id":"n-1","title":"Hi","body":"There"}');
    expect(n?.id).toBe('n-1');
    expect(n?.title).toBe('Hi');
  });

  it('joins data split over several lines', () => {
    expect(parseNotificationEvent('event: notification\ndata: {"id":"n-2",\ndata: "title":"Split"}')?.title).toBe('Split');
  });

  it('ignores heartbeats, comments and other events', () => {
    expect(parseNotificationEvent(':heartbeat')).toBeNull();
    expect(parseNotificationEvent('event:other\ndata:{}')).toBeNull();
    expect(parseNotificationEvent('event:notification')).toBeNull();
  });
});
