<template>
    <div class="responsePanel">
        <div class="response-btns">
            <el-tooltip class="item" effect="dark" content="use Result Structure" placement="top-end">
                <el-checkbox style="padding: 3px 5px;z-index: 1000" v-model="resultStructureCopy" v-if="onEditPage">Structure</el-checkbox>
            </el-tooltip>
            <el-button-group>
                <el-tooltip class="item" effect="dark" content="Copy to Clipboard" placement="top-end">
                    <el-button class="z-index-top" size="mini" round
                               v-clipboard:copy="responseBodyCopy"
                               v-clipboard:success="handleJsonResultCopySuccess"
                               v-clipboard:error="handleJsonResultCopyError">
                        <svg class="icon" aria-hidden="true">
                            <use xlink:href="#iconcopy"></use>
                        </svg>
                    </el-button>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="Format Result" placement="top-end">
                    <el-button class="z-index-top" size="mini" round
                               @click.native='handleJsonResultFormatter' v-if="panelActiveName ==='result_view' && resultType ==='json'">
                        <svg class="icon" aria-hidden="true">
                            <use xlink:href="#iconformat"></use>
                        </svg>
                    </el-button>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="Save As Download" placement="top-end">
                    <el-button class="z-index-top" size="mini" round
                               @click.native='handleResultDownload' v-if="panelActiveName ==='result_view' && resultType ==='bytes'">
                        <svg class="icon" aria-hidden="true">
                            <use xlink:href="#icondownload"></use>
                        </svg>
                    </el-button>
                </el-tooltip>
                <el-tooltip class="item" effect="dark" content="Format Structure" placement="top-end">
                    <el-button class="z-index-top" size="mini" round
                               @click.native='handleStructureFormatter' v-if="onEditPage && panelActiveName ==='result_format'">
                        <svg class="icon" aria-hidden="true">
                            <use xlink:href="#iconformat"></use>
                        </svg>
                    </el-button>
                </el-tooltip>
            </el-button-group>
        </div>
        <el-tabs class="response-tabs" type="card" v-model="panelActiveName">
            <el-tab-pane name="result_view" label="Result">
                <div :id="id + '_responseBodyRef'">
                    <codemirror v-model="responseBodyCopy" :options="defaultOption"/>
                </div>
            </el-tab-pane>
            <el-tab-pane name="result_format" label="Structure" v-if="onEditPage" :disabled="!resultStructureCopy">
                <div :id="id + '_responseFormatRef'">
                    <codemirror v-model="responseFormatCopy" :options="defaultOption"/>
                </div>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>
<script>
    import 'codemirror'
    import {formatDate} from "../utils/utils"


    export default {
        props: {
            id: {
                type: String,
                default: function () {
                    return 'responsePanel'
                }
            },
            responseBody: {
                type: String,
                default: function () {
                    return '"empty."'
                }
            },
            responseFormat: {
                type: String,
                default: function () {
                    return '{\n' +
                        '  "success"      : "@resultStatus",\n' +
                        '  "message"      : "@resultMessage",\n' +
                        '  "code"         : "@resultCode",\n' +
                        '  "lifeCycleTime": "@timeLifeCycle",\n' +
                        '  "executionTime": "@timeExecution",\n' +
                        '  "value"        : "@resultData"\n' +
                        '}'
                }
            },
            resultStructure: {
                type: Boolean,
                default: function () {
                    return true
                }
            },
            onEditPage: {
                type: Boolean,
                default: function () {
                    return false; // 是否在 编辑 页面
                }
            },
            resultType: {
                type: String,
                default: function () {
                    return 'json';
                }
            }
        },
        data() {
            return {
                defaultOption: {
                    tabSize: 4,
                    styleActiveLine: true,
                    lineNumbers: true,
                    line: true,
                    mode: 'text/javascript'
                },
                responseBodyCopy: '',
                responseFormatCopy: '',
                resultStructureCopy: true,
                panelActiveName: 'result_view',
                height: "10px"
            }
        },
        mounted() {
            this.responseBodyCopy = this.responseBody;
            this.resultStructureCopy = (this.resultStructure === undefined) ? true : this.resultStructure;
            this.responseFormatCopy = this.responseFormat;
        },
        watch: {
            'responseBodyCopy': {
                handler(val, oldVal) {
                    this.$emit('onResponseBodyChange', this.responseBodyCopy)
                }
            },
            'resultStructureCopy': {
                handler(val, oldVal) {
                    if (!this.resultStructureCopy) {
                        this.panelActiveName = 'result_view'
                    }
                    this.$emit('onResultStructureChange', this.resultStructureCopy)
                }
            },
            'responseFormatCopy': {
                handler(val, oldVal) {
                    if (this.resultStructureCopy) {
                        this.$emit('onResultStructureFormatChange', this.responseFormatCopy)
                    }
                }
            }
        },
        methods: {
            // 响应结果格式化
            handleJsonResultFormatter() {
                try {
                    this.responseBodyCopy = JSON.stringify(JSON.parse(this.responseBodyCopy), null, 2)
                } catch (e) {
                    this.$message.error('JsonResult Format Error : ' + e)
                }
            },
            // 响应结果格式化
            handleStructureFormatter() {
                try {
                    this.responseFormatCopy = JSON.stringify(JSON.parse(this.responseFormatCopy), null, 2)
                } catch (e) {
                    this.$message.error('Structure Format Error : ' + e)
                }
            },
            // 拷贝结果
            handleJsonResultCopySuccess() {
                this.$message({message: 'JsonResult Copy to Copied', type: 'success'})
            },
            handleJsonResultCopyError() {
                this.$message.error('JsonResult Copy to Copied Failed')
            },
            // 下载
            handleResultDownload() {
                // 把十六进制转换为bytes
                let localResponseBody = this.responseBody;
                let localArrays = localResponseBody.replace(/\n/g, " ").split(" ");
                let byteArray = [];
                for (let i = 0; i < localArrays.length; i++) {
                    byteArray.push(parseInt(localArrays[i], 16));
                }
                let byteUint8Array = new Uint8Array(byteArray);
                // 创建隐藏的可下载链接
                let eleLink = document.createElement('a');
                eleLink.download = formatDate(new Date()) + '.result';
                eleLink.style.display = 'none';
                // 字符内容转变成blob地址
                let blob = new Blob([byteUint8Array]);
                eleLink.href = URL.createObjectURL(blob);
                // 触发点击
                document.body.appendChild(eleLink);
                eleLink.click();
                // 然后移除
                document.body.removeChild(eleLink);
            },
            //
            // 执行布局
            doLayout(height) {
                let responseBodyID = '#' + this.id + '_responseBodyRef';
                let responseBody = document.querySelectorAll(responseBodyID + ' .CodeMirror')[0];
                responseBody.style.height = (height - 47) + 'px'
                //
                if (this.onEditPage) {
                    let responseFormatBodyID = '#' + this.id + '_responseFormatRef';
                    let responseFormatBody = document.querySelectorAll(responseFormatBodyID + ' .CodeMirror')[0];
                    responseFormatBody.style.height = (height - 47) + 'px'
                }
            },
            doUpdate() {
                this.responseBodyCopy = this.responseBody;
                this.resultStructureCopy = (this.resultStructure === undefined) ? true : this.resultStructure;
                this.responseFormatCopy = this.responseFormat;
            }
        }
    }
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

    .responsePanel {
    }

    .response-tabs {
        top: -33px;
        position: relative;
    }

    .response-btns {
        padding: 2px 5px;
        display: flex;
        justify-content: flex-end;

    }

    .z-index-top {
        z-index: 1000;
    }
</style>
