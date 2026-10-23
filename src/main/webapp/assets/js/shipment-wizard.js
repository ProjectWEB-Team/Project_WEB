(() => {
    'use strict';

    const form = document.getElementById('shipment-create-form');
    if (!form) return;

    const steps = Array.from(form.querySelectorAll('[data-shipment-step]'));
    const navigation = document.getElementById('shipment-step-navigation');
    const stepButtons = Array.from(navigation.querySelectorAll('[data-step-target]'));
    const previous = document.getElementById('shipment-previous');
    const next = document.getElementById('shipment-next');
    const quoteButton = document.getElementById('shipment-quote-button');
    const createButton = document.getElementById('shipment-create-button');
    const quote = document.getElementById('shipment-quote');
    const notice = document.getElementById('shipment-quote-notice');
    const summary = document.getElementById('shipment-summary');
    let hasQuote = form.dataset.hasQuote === 'true';
    let current = 0;
    let submitting = false;

    const fieldValue = (name) => form.elements.namedItem(name).value;
    const selectedLabel = (name) => form.elements.namedItem(name).selectedOptions[0]?.textContent || '';

    function updateSummary() {
        const values = {
            sender: `${fieldValue('senderName')} · ${fieldValue('senderPhone')} · ${fieldValue('senderAddress')}`,
            receiver: `${fieldValue('receiverName')} · ${fieldValue('receiverPhone')} · ${fieldValue('receiverAddress')}, ${fieldValue('receiverDistrict')}, ${fieldValue('receiverProvince')}`,
            parcel: `${fieldValue('weightGrams')} g · ${fieldValue('lengthCm')} × ${fieldValue('widthCm')} × ${fieldValue('heightCm')} cm · ${selectedLabel('serviceCode')} · ${selectedLabel('inspectionPolicy')}`,
            values: `${fieldValue('declaredValue')} VND / ${fieldValue('codAmount')} VND`,
            payment: `${selectedLabel('feePayer')} · ${selectedLabel('paymentMethod')}`,
            fee: hasQuote ? quote.querySelector('strong').textContent : 'Cần báo giá lại'
        };
        summary.querySelectorAll('[data-summary]').forEach((item) => {
            item.textContent = values[item.dataset.summary];
        });
    }

    function showStep(index, focus = true) {
        current = index;
        steps.forEach((step, position) => { step.hidden = position !== index; });
        stepButtons.forEach((button, position) => {
            if (position === index) button.setAttribute('aria-current', 'step');
            else button.removeAttribute('aria-current');
            button.disabled = position === 3 && !hasQuote;
        });
        previous.hidden = index === 0;
        next.hidden = index === 3;
        next.disabled = index === 2 && !hasQuote;
        next.textContent = index === 2 ? 'Tiếp tục xác nhận' : 'Tiếp tục';
        quoteButton.disabled = index !== 2 || submitting;
        if (createButton) createButton.disabled = index !== 3 || !hasQuote || submitting;
        document.getElementById('shipment-step-status').textContent = `Bước ${index + 1} / ${steps.length}`;
        if (index === 3) updateSummary();
        if (focus) steps[index].querySelector('h2').focus();
    }

    function validateThrough(index) {
        for (let position = 0; position <= index; position += 1) {
            const invalid = Array.from(steps[position].querySelectorAll('input, select, textarea'))
                .find((field) => field.willValidate && !field.validity.valid);
            if (invalid) {
                showStep(position, false);
                invalid.focus();
                invalid.reportValidity();
                return false;
            }
        }
        return true;
    }

    function goTo(index) {
        if (index > current && !validateThrough(index - 1)) return;
        if (index === 3 && !hasQuote) {
            showStep(2);
            quoteButton.focus();
            return;
        }
        showStep(index);
    }

    function invalidateQuote(event) {
        const field = event.target;
        if (!field.closest('[data-shipment-step]') || field.name === 'pickupMethod' || !hasQuote) return;
        hasQuote = false;
        quote.hidden = true;
        const reference = form.elements.namedItem('quoteReference');
        if (reference) reference.value = '';
        notice.textContent = 'Thông tin đã thay đổi. Vui lòng xem báo giá lại trước khi xác nhận.';
        showStep(current, false);
    }

    previous.addEventListener('click', () => goTo(current - 1));
    next.addEventListener('click', () => goTo(current + 1));
    stepButtons.forEach((button) => {
        button.addEventListener('click', () => goTo(Number(button.dataset.stepTarget)));
    });
    form.addEventListener('input', invalidateQuote);
    form.addEventListener('change', invalidateQuote);
    form.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' && !event.isComposing && event.target.tagName === 'INPUT' && current < 2) {
            event.preventDefault();
            goTo(current + 1);
        }
    });
    form.addEventListener('submit', (event) => {
        if (submitting) {
            event.preventDefault();
            return;
        }
        if (current < 2) {
            event.preventDefault();
            goTo(current + 1);
            return;
        }
        if (!validateThrough(steps.length - 1)) {
            event.preventDefault();
            return;
        }
        if (event.submitter !== quoteButton && !hasQuote) {
            event.preventDefault();
            showStep(2);
            quoteButton.focus();
            return;
        }
        submitting = true;
        quoteButton.disabled = true;
        if (createButton) createButton.disabled = true;
    });

    // Reveal invalid fields before native validation tries to focus a hidden step.
    form.noValidate = true;
    navigation.hidden = false;
    document.getElementById('shipment-step-actions').hidden = false;
    summary.hidden = false;
    if (hasQuote) notice.textContent = 'Đã có báo giá. Tiếp tục để chọn cách gửi hàng và kiểm tra thông tin.';
    const firstError = form.querySelector('.is-invalid');
    form.querySelectorAll('.is-invalid').forEach((field) => {
        field.setAttribute('aria-invalid', 'true');
        const feedback = document.getElementById(`${field.id}.errors`);
        if (feedback) field.setAttribute('aria-describedby', feedback.id);
    });
    const globalErrors = document.getElementById('shipment-form-errors');
    const initialStep = firstError ? Number(firstError.closest('[data-shipment-step]').dataset.shipmentStep)
        : (hasQuote || globalErrors ? 2 : 0);
    showStep(initialStep, false);
    if (firstError) firstError.focus();
    else if (globalErrors) globalErrors.focus();

    window.addEventListener('pageshow', () => {
        submitting = false;
        showStep(current, false);
    });
})();
