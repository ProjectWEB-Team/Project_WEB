(function () {
    var button = document.getElementById('uteSidebarToggle');
    var sidebar = document.getElementById('uteSidebar');
    if (!button || !sidebar) return;

    var key = 'ute-sidebar-collapsed';
    var collapsed = false;
    var lastFocusedControl;
    try { collapsed = sessionStorage.getItem(key) === 'true'; } catch (e) {}
    document.addEventListener('focusin', function (event) { lastFocusedControl = event.target; });

    function render() {
        document.body.classList.toggle('ute-sidebar-collapsed', collapsed);
        button.setAttribute('aria-expanded', String(!collapsed));
        button.textContent = collapsed ? 'Mở menu' : 'Thu gọn menu';
    }

    render();
    button.parentElement.hidden = false;
    button.addEventListener('click', function () {
        collapsed = !collapsed;
        render();
        try { sessionStorage.setItem(key, String(collapsed)); } catch (e) {}
    });

    window.matchMedia('(min-width: 992px)').addEventListener('change', function (event) {
        // CSS may blur a hidden control before the breakpoint event runs.
        var focused = document.activeElement === document.body ? lastFocusedControl : document.activeElement;
        if (event.matches && collapsed && sidebar.contains(focused)) {
            button.focus();
        } else if (!event.matches && (focused === button || sidebar.contains(focused))) {
            sidebar.querySelector('.ute-menu-btn').focus();
        }
    });
}());
