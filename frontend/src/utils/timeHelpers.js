/**
 * Formats user's leave balance from total hours to "X Day Y Hour" string.
 * Assumes 8 hours = 1 day.
 * @param {number} totalHours - Total leave balance in hours.
 * @returns {string} Formatted string (e.g., "14 Gün 4 Saat").
 */
export const formatLeaveDuration = (totalHours) => {
    if (!totalHours && totalHours !== 0) return '0 Gün';
    
    const days = Math.floor(totalHours / 8);
    const hours = totalHours % 8;

    let result = '';
    
    if (days > 0) {
        result += `${days} Gün `;
    }
    
    if (hours > 0) {
        result += `${hours} Saat`;
    }
    
    if (days === 0 && hours === 0) {
        return '0 Saat';
    }

    return result.trim();
};

/**
 * Formats a date string to a more readable format (DD.MM.YYYY).
 * @param {string} dateString 
 * @returns {string}
 */
export const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('tr-TR').format(date);
};
