import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

// Self-service requests: business errors (code != 0) reach the callback so the page can show
// the backend message; network errors become a synthetic { code: -1 } response.
function sendSelfServiceRequest(url, method, data, callback) {
    const request = RequestService.sendRequest()
        .url(url)
        .method(method)
        .success((res) => {
            RequestService.clearRequestTime();
            callback(res);
        })
        .fail((res) => {
            RequestService.clearRequestTime();
            callback(res);
        })
        .networkFail((err) => {
            console.error(`Voice clone request failed: ${method} ${url}`, err);
            RequestService.clearRequestTime();
            callback({ data: { code: -1, msg: (err && err.msg) || '' } });
        });
    if (data !== undefined) {
        request.data(data);
    }
    request.send();
}

export default {
    // Create a clone slot on one of the user's own ElevenLabs models; body: { modelId, name, languages? }
    createOwnVoiceClone(body, callback) {
        sendSelfServiceRequest(`${getServiceUrl()}/voiceClone/mine`, 'POST', body, callback);
    },

    // Delete a self-managed clone slot (and its ElevenLabs voice)
    deleteVoiceClone(id, callback) {
        sendSelfServiceRequest(`${getServiceUrl()}/voiceClone/${encodeURIComponent(id)}`, 'DELETE', undefined, callback);
    },

    // Query voice resources with pagination
    getVoiceCloneList(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/voiceClone`)
            .method('GET')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('Failed to get voice list:', err);
                RequestService.reAjaxFun(() => {
                    this.getVoiceCloneList(params, callback);
                });
            }).send();
    },

    // Upload audio file
    uploadVoice(formData, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/voiceClone/upload`)
            .method('POST')
            .data(formData)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('Failed to upload audio:', err);
                RequestService.reAjaxFun(() => {
                    this.uploadVoice(formData, callback);
                });
            }).send();
    },

    // Update voice name
    updateName(params, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/voiceClone/updateName`)
            .method('POST')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('Failed to update name:', err);
                RequestService.reAjaxFun(() => {
                    this.updateName(params, callback);
                });
            }).send();
    },

    // Get audio download ID
    getAudioId(id, callback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/voiceClone/audio/${id}`)
            .method('POST')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .networkFail((err) => {
                console.error('Failed to get audio ID:', err);
                RequestService.reAjaxFun(() => {
                    this.getAudioId(id, callback);
                });
            }).send();
    },

    // Get audio playback URL
    getPlayVoiceUrl(uuid) {
        return `${getServiceUrl()}/voiceClone/play/${uuid}`;
    },

    // Clone audio
    cloneAudio(params, callback, errorCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/voiceClone/cloneAudio`)
            .method('POST')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((res) => {
                // Business failure callback
                RequestService.clearRequestTime();
                if (errorCallback) {
                    errorCallback(res);
                } else {
                    callback(res);
                }
            })
            .networkFail((err) => {
                console.error('Upload failed:', err);
                RequestService.reAjaxFun(() => {
                    this.cloneAudio(params, callback, errorCallback);
                });
            }).send();
    }
}
