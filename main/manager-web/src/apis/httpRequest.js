import Fly from 'flyio/dist/npm/fly';
import store from '../store/index';
import Constant from '../utils/constant';
import { getServiceUrl } from '../apis/api';
import { goToPage, isNotNull, showDanger, showWarning } from '../utils/index';
import i18n from '../i18n/index';

const fly = new Fly()
// Set timeout
fly.config.timeout = 30000

// Separate fly instance for silent refresh, so no callback chain leads back into sendRequest
const refreshFly = new Fly()
refreshFly.config.timeout = 30000

// Silent refresh: swap the token when under 1h remains; a single-flight lock avoids concurrent refreshes
const REFRESH_AHEAD_MS = 60 * 60 * 1000
let refreshPromise = null

function getTokenExpireAt() {
    // Read localStorage like persistTokenExpireAt does (the store uses its cached state.token,
    // so right after a localStorage write it would miss the added _localStoredAt)
    const raw = localStorage.getItem('token')
    if (!raw) return 0
    try {
        const parsed = JSON.parse(raw)
        if (!parsed || !parsed.expire) return 0
        // login.vue stores the token via setToken without a createdAt field;
        // use _localStoredAt as the local issue-time baseline
        return (parsed._localStoredAt || 0) + parsed.expire * 1000
    } catch (e) {
        return 0
    }
}

// Record when this token was first seen; update localStorage and the store together so they stay in sync
function persistTokenExpireAt() {
    const raw = localStorage.getItem('token')
    if (!raw) return
    try {
        const parsed = JSON.parse(raw)
        if (parsed && parsed.expire && !parsed._localStoredAt) {
            parsed._localStoredAt = Date.now()
            const next = JSON.stringify(parsed)
            localStorage.setItem('token', next)
            store.commit('setToken', next)
        }
    } catch (e) { /* ignore */ }
}

function shouldRefresh() {
    persistTokenExpireAt()
    const exp = getTokenExpireAt()
    if (!exp) return false
    return exp - Date.now() < REFRESH_AHEAD_MS
}

function doRefresh() {
    // Call fly.request directly, bypassing sendRequest, so silent refresh cannot loop
    const raw = localStorage.getItem('token')
    if (!raw) return Promise.reject(new Error('no token'))
    let bearer
    try {
        bearer = JSON.parse(raw).token
    } catch (e) {
        return Promise.reject(e)
    }
    return refreshFly.request(getServiceUrl() + '/user/refresh', {}, {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + bearer }
    }).then((res) => {
        const body = res && res.data
        if (!body || (body.code !== 0 && body.code !== 'success')) {
            throw new Error('refresh failed: ' + (body && body.msg))
        }
        const newDto = body.data
        newDto._localStoredAt = Date.now()
        store.commit('setToken', JSON.stringify(newDto))
        return newDto
    })
}

function triggerSilentRefresh() {
    if (!refreshPromise) {
        refreshPromise = doRefresh().catch((err) => {
            store.commit('clearAuth')
            goToPage(Constant.PAGE.LOGIN, true)
            throw err
        }).finally(() => {
            refreshPromise = null
        })
    }
    return refreshPromise
}

/**
 * Request service wrapper
 */
export default {
    sendRequest,
    reAjaxFun,
    clearRequestTime,
    triggerSilentRefresh
}

function sendRequest() {
    return {
        _sucCallback: null,
        _failCallback: null,
        _networkFailCallback: null,
        _method: 'GET',
        _data: {},
        _header: { 'content-type': 'application/json; charset=utf-8' },
        _url: '',
        _responseType: undefined, // Added response type field
        'send'() {
            // Set the language request header
            const currentLang = i18n.locale;
            // Convert language code format, e.g. zh_CN to zh-CN
            let acceptLanguage = currentLang.replace('_', '-');
            // Add a default region code for English
            if (acceptLanguage === 'en') {
                acceptLanguage = 'en-US';
            }
            this._header['Accept-Language'] = acceptLanguage;

            const doSend = () => {
                if (isNotNull(store.getters.getToken)) {
                    this._header.Authorization = 'Bearer ' + (JSON.parse(store.getters.getToken)).token
                }

                // Print request info
                fly.request(this._url, this._data, {
                    method: this._method,
                    headers: this._header,
                    responseType: this._responseType
                }).then((res) => {
                    const error = httpHandlerError(res, this._failCallback, this._networkFailCallback);
                    if (error) {
                        return
                    }

                    if (this._sucCallback) {
                        this._sucCallback(res)
                    }
                }).catch((res) => {
                    // Print the failure response
                    console.log('catch', res)
                    httpHandlerError(res, this._failCallback, this._networkFailCallback)
                })
            }

            // If the token expires within 1h, silently refresh it first so the request carries a fresh token
            if (shouldRefresh()) {
                triggerSilentRefresh().then(doSend).catch(() => {
                    // A failed refresh redirects to login; nothing more to do here
                })
            } else {
                doSend()
            }
            return this
        },
        'success'(callback) {
            this._sucCallback = callback
            return this
        },
        'fail'(callback) {
            this._failCallback = callback
            return this
        },
        'networkFail'(callback) {
            this._networkFailCallback = callback
            return this
        },
        'url'(url) {
            if (url) {
                url = url.replaceAll('$', '/')
            }
            this._url = url
            return this
        },
        'data'(data) {
            this._data = data
            return this
        },
        'method'(method) {
            this._method = method
            return this
        },
        'header'(header) {
            this._header = header
            return this
        },
        'showLoading'(showLoading) {
            this._showLoading = showLoading
            return this
        },
        'async'(flag) {
            this.async = flag
        },
        // Added method to set the response type
        'type'(responseType) {
            this._responseType = responseType;
            return this;
        }
    }
}

/**
 * Info: information returned after the request completes
 * failCallback: callback function
 * networkFailCallback: callback function
 */
// Add logging within the error handler function
function httpHandlerError(info, failCallback, networkFailCallback) {

    /** Request succeeded, exit this function. You can decide success based on project needs. Here success is when status is 200 */
    let networkError = false
    if (info.status === 200) {
        if (info.data.code === 'success' || info.data.code === 0 || info.data.code === undefined) {
            return networkError
        } else if (info.data.code === 401) {
            store.commit('clearAuth');
            goToPage(Constant.PAGE.LOGIN, true);
            return true
        } else {
            // Use the internationalized message returned by the backend directly
            let errorMessage = info.data.msg;
            
            if (failCallback) {
                failCallback(info)
            } else {
                showDanger(errorMessage)
            }
            return true
        }
    }
    if (networkFailCallback) {
        networkFailCallback(info)
    } else {
        showDanger(`A network request error occurred [${info.status}]`)
    }
    return true
}

let requestTime = 0
let reAjaxSec = 2

function reAjaxFun(fn) {
    let nowTimeSec = new Date().getTime() / 1000
    if (requestTime === 0) {
        requestTime = nowTimeSec
    }
    let ajaxIndex = parseInt((nowTimeSec - requestTime) / reAjaxSec)
    if (ajaxIndex > 10) {
        showWarning('Unable to connect to the server')
    } else {
        showWarning('Connecting to the server(' + ajaxIndex + ')')
    }
    if (ajaxIndex < 10 && fn) {
        setTimeout(() => {
            fn()
        }, reAjaxSec * 1000)
    }
}

function clearRequestTime() {
    requestTime = 0
}