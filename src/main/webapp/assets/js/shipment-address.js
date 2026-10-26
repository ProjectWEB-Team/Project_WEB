(() => {
    'use strict';

    const form = document.getElementById('shipment-create-form');
    if (!form) return;
    const groups = Array.from(document.querySelectorAll('[data-address-group]'));
    let catalog = null;
    const regionNames = { NORTH: 'Miền Bắc', CENTRAL: 'Miền Trung', SOUTH: 'Miền Nam' };
    const key = value => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase().trim();

    groups.forEach(group => {
        const who = group.dataset.addressGroup;
        const province = document.getElementById(`${who}ProvinceCode`);
        const ward = document.getElementById(`${who}WardCode`);
        const search = document.getElementById(`${who}WardSearch`);
        const status = document.getElementById(`${who}-area-status`);
        function update(selectedWard) {
            if (!catalog) return;
            const area = catalog.find(item => item.code === province.value);
            const options = area?.wards || [];
            const filtered = options.filter(item => item.code === selectedWard || key(item.name).includes(key(search.value)));
            ward.replaceChildren(new Option(area ? 'Chọn phường/xã/đặc khu' : 'Chọn tỉnh/thành trước', ''));
            filtered.forEach(item => ward.add(new Option(item.name, item.code)));
            ward.value = options.some(item => item.code === selectedWard) ? selectedWard : '';
            ward.disabled = !area;
            search.disabled = !area;
            status.textContent = area
                ? `${regionNames[area.region]} · ${filtered.length} phường/xã/đặc khu trong danh sách.`
                : 'Chọn tỉnh/thành để xem phường/xã/đặc khu.';
        }
        group.addressUpdate = () => update(ward.value);
        province.addEventListener('change', () => {
            search.value = '';
            ward.replaceChildren(new Option('Chọn phường/xã/đặc khu', ''));
            ward.disabled = true;
            search.disabled = true;
            update('');
        });
        search.addEventListener('input', () => update(ward.value));
        ward.disabled = !province.value;
        search.disabled = !province.value;
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
