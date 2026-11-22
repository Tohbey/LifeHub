import { splitTags } from './notes';

describe('splitTags', () => {
  it('splits on commas, trims and drops empty entries, keeping at most 10', () => {
    expect(splitTags(' Home, admin,, home ')).toEqual(['Home', 'admin', 'home']);
    expect(splitTags('')).toEqual([]);
    expect(splitTags(Array.from({ length: 12 }, (_, i) => `t${i}`).join(','))).toHaveLength(10);
  });
});
