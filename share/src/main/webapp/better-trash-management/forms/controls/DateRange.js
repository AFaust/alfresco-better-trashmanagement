/**
 * This specialisation of the DateRange widget deals with some shortcomings of the base widget. It contains an alternate validation logic
 * for from/to dates, specifically catering for open-ended use cases where either constituent field being not set is definitely NOT an
 * invalid state.
 * 
 * @module better-trash-management/forms/controls/DateRange
 * @extends module:alfresco/forms/controls/DateRange
 * @author Axel Faust
 */
define([ 'dojo/_base/declare', 'alfresco/forms/controls/DateRange' ], function(declare, DateRange)

{
    return declare([ DateRange ], {

        allowOpenEndDateRange : false,

        validateFromIsBeforeTo : function betterTrashManagement_forms_controls_DateRange__validateFromIsBeforeTo(validationConfig)
        {
            var isValid, value, valueTokens, fromValue, toValue;

            isValid = true;
            value = this.getValue();
            if (value)
            {
                valueTokens = value.split(this.dateSeparator);
                fromValue = '';
                toValue = '';

                if (valueTokens.length === 2)
                {
                    fromValue = valueTokens[0];
                    toValue = valueTokens[1];
                }

                if (fromValue !== '' && toValue !== '')
                {
                    // If both pickers have a date, compare the values...
                    isValid = new Date(fromValue) < new Date(toValue);
                }
                else if (!this.allowOpenEndDateRange && (fromValue !== '' || toValue !== ''))
                {
                    // if one picker has a value, then it's in the invalid state...
                    isValid = false;
                }
                // ...else either both pickers have no value or an open-ended range is allowed, so it's valid
            }
            this.reportValidationResult(validationConfig, isValid);
        }
    });
});
