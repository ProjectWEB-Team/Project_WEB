(() => {
    'use strict';

    const form = document.getElementById('shipment-create-form');
    if (!form) return;
    const groups = Array.from(document.querySelectorAll('[data-address-group]'));
    let catalog = null;
    const regionNames = { NORTH: 'Miền Bắc', CENTRAL: 'Miền Trung', SOUTH: 'Miền Nam' };

    groups.forEach(group => {
        const who = group.dataset.addressGroup;
        const province = document.getElementById(`${who}ProvinceCode`);
        const ward = document.getElementById(`${who}WardCode`);
        const status = document.getElementById(`${who}-area-status`);
        function update(selectedWard) {
            if (!catalog) return;
            const area = catalog.find(item => item.code === province.value);
            const options = area?.wards || [];
            ward.replaceChildren(new Option(area ? 'Chọn phường/xã/đặc khu' : 'Chọn tỉnh/thành trước', ''));
            options.forEach(item => ward.add(new Option(item.name, item.code)));
            ward.value = options.some(item => item.code === selectedWard) ? selectedWard : '';
            ward.disabled = !area;
            status.textContent = area
                ? `${regionNames[area.region]} · ${options.length} phường/xã/đặc khu trong danh sách.`
                : 'Chọn tỉnh/thành để xem phường/xã/đặc khu.';
        }
        group.addressUpdate = () => update(ward.value);
        province.addEventListener('change', () => {
            ward.replaceChildren(new Option('Chọn phường/xã/đặc khu', ''));
            ward.disabled = !province.value;
            update('');
        });
        ward.disabled = !province.value;
        status.textContent = 'Đang tải danh sách địa chỉ…';
    });

    fetch(form.dataset.addressCatalogUrl, { headers: { Accept: 'application/json' } })
        .then(response => {
            if (!response.ok) throw new Error('catalog unavailable');
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data) || data.length !== 34) throw new Error('invalid catalog');
            catalog = data;
            groups.forEach(group => group.addressUpdate());
        })
        .catch(() => {
            groups.forEach(group => {
                document.getElementById(`${group.dataset.addressGroup}-area-status`).textContent = 'Không tải được danh sách địa chỉ. Vui lòng tải lại trang hoặc đăng nhập lại.';
            });
        });
    window.addEventListener('pageshow', () => groups.forEach(group => group.addressUpdate()));
})();
