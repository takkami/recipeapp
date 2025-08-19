// サイドバーのアクティブ状態管理（修正版）

// モーダル表示前の状態を保存する変数
let previousActiveState = null;

function updateSidebarActiveState() {
    const currentPath = window.location.pathname;
    const sidebarItems = document.querySelectorAll('.sidebar ul li');

    // すべてのアクティブ状態をリセット
    sidebarItems.forEach(item => {
        item.classList.remove('active');
    });

    // カテゴリビューが表示されているかチェック
    const categoriesView = document.getElementById('categoriesView');
    const isCategoriesViewVisible = categoriesView && categoriesView.style.display === 'block';

    // 現在のページに応じてアクティブ状態を設定
    sidebarItems.forEach(item => {
        const link = item.querySelector('a');
        const menuItem = item.querySelector('.menu-item');

        if (link) {
            const href = link.getAttribute('href');
            const spanText = link.querySelector('span')?.textContent;

            // お気に入りページの判定
            if (currentPath === '/recipes/favorites' && href === '/recipes/favorites' && !isCategoriesViewVisible) {
                item.classList.add('active');
            }
            // ランダムレシピページの判定
            else if (currentPath === '/recipes/random' && href === '/recipes/random' && !isCategoriesViewVisible) {
                item.classList.add('active');
            }
            // ホームページでホームボタンの判定
            else if ((currentPath === '/home' || currentPath === '/') && href === '/home' && spanText === 'ホーム' && !isCategoriesViewVisible) {
                item.classList.add('active');
            }
        }

        // メニューアイテム（JavaScript機能）の判定
        if (menuItem) {
            const menuText = menuItem.querySelector('span')?.textContent;

            // カテゴリ一覧が表示されている場合のみ
            if (isCategoriesViewVisible && menuText === 'カテゴリ一覧') {
                item.classList.add('active');
            }
        }
    });

    // レシピフォームページの場合（新規追加・編集）
    if (currentPath.includes('/recipes/new') || currentPath.includes('/recipes/edit')) {
        // 特定のメニューをアクティブにしない
    }
}

// 現在のアクティブ状態を保存する関数
function saveActiveState() {
    const activeItem = document.querySelector('.sidebar ul li.active');
    if (activeItem) {
        const link = activeItem.querySelector('a');
        const menuItem = activeItem.querySelector('.menu-item');

        if (link) {
            const href = link.getAttribute('href');
            const spanText = link.querySelector('span')?.textContent;
            previousActiveState = { type: 'link', href, spanText };
        } else if (menuItem) {
            const spanText = menuItem.querySelector('span')?.textContent;
            previousActiveState = { type: 'menu', spanText };
        }
    } else {
        previousActiveState = null;
    }

    console.log('保存されたアクティブ状態:', previousActiveState);
}

// 保存されたアクティブ状態を復元する関数
function restoreActiveState() {
    if (!previousActiveState) {
        updateSidebarActiveState();
        return;
    }

    console.log('復元するアクティブ状態:', previousActiveState);

    const sidebarItems = document.querySelectorAll('.sidebar ul li');

    // すべてのアクティブ状態をリセット
    sidebarItems.forEach(item => {
        item.classList.remove('active');
    });

    // 保存された状態に基づいて復元
    sidebarItems.forEach(item => {
        const link = item.querySelector('a');
        const menuItem = item.querySelector('.menu-item');

        if (previousActiveState.type === 'link' && link) {
            const href = link.getAttribute('href');
            const spanText = link.querySelector('span')?.textContent;

            if (href === previousActiveState.href && spanText === previousActiveState.spanText) {
                item.classList.add('active');
                console.log('リンクアクティブ状態復元:', spanText);
            }
        } else if (previousActiveState.type === 'menu' && menuItem) {
            const spanText = menuItem.querySelector('span')?.textContent;

            if (spanText === previousActiveState.spanText) {
                item.classList.add('active');
                console.log('メニューアクティブ状態復元:', spanText);
            }
        }
    });

    // 復元後は保存状態をクリア
    previousActiveState = null;
}

// カテゴリページ表示時のアクティブ状態更新（home.htmlでのみ使用）
function showCategoriesPageWithActive() {
    // showCategoriesPage関数が存在する場合のみ実行
    if (typeof showCategoriesPage === 'function') {
        saveActiveState(); // 現在の状態を保存
        showCategoriesPage();
        // カテゴリ表示後にアクティブ状態を更新
        setTimeout(updateSidebarActiveState, 100);
    }
}

function hideCategoriesPageWithActive() {
    // hideCategoriesPage関数が存在する場合のみ実行
    if (typeof hideCategoriesPage === 'function') {
        hideCategoriesPage();
        // カテゴリ非表示後にアクティブ状態を復元
        setTimeout(restoreActiveState, 100);
    }
}

// モーダル表示時のアクティブ状態更新
function showUserInfoWithActive() {
    // showUserInfo関数が存在する場合のみ実行
    if (typeof showUserInfo === 'function') {
        saveActiveState(); // 現在の状態を保存
        showUserInfo();
        // ユーザー情報表示中はサイドバーアクティブ状態を一時的に変更
        setTimeout(() => {
            document.querySelectorAll('.sidebar ul li').forEach(item => {
                item.classList.remove('active');
                if (item.querySelector('.menu-item span')?.textContent === 'ユーザー情報') {
                    item.classList.add('active');
                }
            });
        }, 100);
    }
}

function showSettingsWithActive() {
    // showSettings関数が存在する場合のみ実行
    if (typeof showSettings === 'function') {
        saveActiveState(); // 現在の状態を保存
        showSettings();
        // 設定表示中はサイドバーアクティブ状態を一時的に変更
        setTimeout(() => {
            document.querySelectorAll('.sidebar ul li').forEach(item => {
                item.classList.remove('active');
                if (item.querySelector('.menu-item span')?.textContent === '設定') {
                    item.classList.add('active');
                }
            });
        }, 100);
    }
}

function showHelpWithActive() {
    // showHelp関数が存在する場合のみ実行
    if (typeof showHelp === 'function') {
        saveActiveState(); // 現在の状態を保存
        showHelp();
        // ヘルプ表示中はサイドバーアクティブ状態を一時的に変更
        setTimeout(() => {
            document.querySelectorAll('.sidebar ul li').forEach(item => {
                item.classList.remove('active');
                if (item.querySelector('.menu-item span')?.textContent === 'ヘルプ') {
                    item.classList.add('active');
                }
            });
        }, 100);
    }
}

// モーダルが閉じられた時にアクティブ状態を復元
function closeModalWithActive() {
    // closeModal関数が存在する場合のみ実行
    if (typeof closeModal === 'function') {
        closeModal();
        setTimeout(restoreActiveState, 100);
    } else if (typeof closeFormModal === 'function') {
        // recipe_form.htmlの場合
        closeFormModal();
        setTimeout(restoreActiveState, 100);
    }
}

// サイドバーメニューの初期化
function initializeSidebarActiveState() {
    // ページ読み込み時にアクティブ状態を設定
    updateSidebarActiveState();

    // 既存のメニューアイテムのonclickを更新
    const menuItems = document.querySelectorAll('.sidebar .menu-item');
    menuItems.forEach(item => {
        const span = item.querySelector('span');
        if (span) {
            const text = span.textContent;

            switch(text) {
                case 'カテゴリ一覧':
                    // 既存のonclickをアクティブ状態管理付きに置き換え
                    item.onclick = function() {
                        showCategoriesPageWithActive();
                    };
                    break;
                case 'ユーザー情報':
                    item.onclick = function() {
                        showUserInfoWithActive();
                    };
                    break;
                case 'ヘルプ':
                    item.onclick = function() {
                        showHelpWithActive();
                    };
                    break;
                case '設定':
                    item.onclick = function() {
                        showSettingsWithActive();
                    };
                    break;
            }
        }
    });

    // モーダルクローズボタンの更新（存在する場合のみ）
    setTimeout(() => {
        const modalCloseButtons = document.querySelectorAll('.modal-close');
        modalCloseButtons.forEach(button => {
            // 既存のonclickハンドラーを保持しつつ、アクティブ状態管理を追加
            const originalOnclick = button.onclick;
            button.onclick = function() {
                closeModalWithActive();
            };
        });
    }, 100);
}

// ページのナビゲーション時にアクティブ状態を更新
function handleNavigationActiveState() {
    // ページ遷移時やブラウザの戻る/進むボタンでアクティブ状態を更新
    window.addEventListener('popstate', updateSidebarActiveState);

    // サイドバーのリンククリック時にアクティブ状態を更新
    document.querySelectorAll('.sidebar a').forEach(link => {
        link.addEventListener('click', () => {
            setTimeout(updateSidebarActiveState, 100);
        });
    });
}

// DOMContentLoadedイベントで初期化
document.addEventListener('DOMContentLoaded', function() {
    initializeSidebarActiveState();
    handleNavigationActiveState();
});

// ページの状態変化を監視（SPAの場合）
if (typeof MutationObserver !== 'undefined') {
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            // categoriesViewの表示状態変化を監視
            if (mutation.type === 'attributes' &&
                mutation.attributeName === 'style' &&
                mutation.target.id === 'categoriesView') {
                setTimeout(updateSidebarActiveState, 50);
            }
        });
    });

    // カテゴリビューの監視を開始（要素が存在する場合のみ）
    setTimeout(() => {
        const categoriesView = document.getElementById('categoriesView');
        if (categoriesView) {
            observer.observe(categoriesView, {
                attributes: true,
                attributeFilter: ['style']
            });
        }
    }, 100);
}