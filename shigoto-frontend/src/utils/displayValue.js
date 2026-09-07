/**
 * Derives has display value without mutating application state.
 */
export function hasDisplayValue(value) {
  return value !== null
    && value !== undefined
    && (typeof value !== 'string' || value.trim() !== '');
}
