import {BrowserWindow} from 'electron';
import { Tokenizer } from "./model/tokenizer";

export default class Main {
    static mainWindow: Electron.BrowserWindow;
    static application: Electron.App;
    static BWindow: any;

    private static onWindowAllClosed() {
        if (process.platform !== 'darwin') {
            Main.application.quit();
        }
    }

    private static onReady() {
        Main.mainWindow = new Main.BWindow({ width: 1800, height: 1200, webPreferences: { nodeIntegration: true }, darkTheme: true });
        Main.mainWindow.loadFile('./index.html').then(r => console.log(r));
        Main.mainWindow.on('closed', Main.onWindowAllClosed);
    }

    static main(app: Electron.App, browserWindow: typeof BrowserWindow) {
        Main.BWindow = browserWindow;
        Main.application = app;
        Main.application.on('window-all-closed', Main.onWindowAllClosed);
        Main.application.on('ready', Main.onReady);
    }
}


/** ************************************************************************************* */
function algo(input: any) {
    let inFile: HTMLElement | null  = document.getElementById("inputFile");
    let outText: HTMLElement | null = document.getElementById("output");
    if (outText && inFile) {
        outText.style.display = 'block';
        isOpenFile();
    }

    const RGX = {
        enter: /\r?\n/gm,
        import: /((?:import ))(\S*)/,
        class: /(class |(?:public|private|protected)+ class )(\S*)/,
        enum: /(enum |(?:public|private|protected)+ enum )(\S*)/,
        interface: /(interface |(?:public|private|protected)+ interface )(\S*)/,
        interface2: /((?:implements ))(\S*)/,
        reserved: /(if|else|for|try|cath|return|new)(\s)/,
        attribute: /((?:public|private|protected) \S* )(\S*;)/,
        method: /((?:public|private|protected) \S* )(\S*\()/
    };
    let file = input.files[0];
    let reader = new FileReader();
    reader.readAsText(file);
    let alguito = document.getElementById('output');
    reader.onload = function() {
        if (alguito && typeof reader.result === "string") {
            let array = reader.result.split(RGX.enter);
            // let line = reader.result;
            let result: string = "";
            let number = 1;
            array.forEach(line => {
                line = regexResolver(line, RGX.attribute, 'rgb(123,207,199)','attribute');
                line = regexResolver(line, RGX.method, 'rgb(73,212,15)','method');
                line = regexResolver(line, RGX.import, 'green', 'import');
                line = regexResolver(line, RGX.class, 'rgb(94,214,144)','class');
                line = regexResolver(line, RGX.enum, 'rgb(15,160,160)','enum');
                line = regexResolver(line, RGX.interface, 'rgb(143,148,8)','interface');
                line = regexResolver(line, RGX.interface2, 'rgb(143,148,8)','interface');
                line = regexResolver(line, RGX.reserved, 'rgb(109,110,233)');
                result = result + '\n' + '<a class="number">'+number++ + '</a>' + '<a class="line">'+line+'</a>';
            });
            // alguito.innerHTML = line;
            alguito.innerHTML = result;
        }
    };

    reader.onerror = function() {
        console.log(reader.error);
    };
}

function regexResolver(line: string, regex: RegExp, color: string, element: string = ''): string {
    let data = element === '' ? undefined : document.getElementById(element);
    if (regex.test(line) && line.match(regex)) {
        line = line.replace(regex ,`<p style="color: ${color}">$1</p>$2`);

        if (data) {
            // @ts-ignore
            data.innerHTML += line.match(regex)[2].replace(/(?:{|;|\()/gm, '') + '\n';
            data.style.display = 'block';
        }
    }
    return line;
}

let isOpened = false;
function isOpenFile() {
    const open = document.getElementById('open');
    const close = document.getElementById('close');
    if (open && close) {
        open.style.display = isOpened ? 'flex' : 'none';
        close.style.display = !isOpened ? 'flex' : 'none';
        isOpened = !isOpened;
    }
}

function closeFile(keepButton: boolean = false) {
    const fields: string[] = ['method', 'import','class', 'enum','interface', 'attribute'];
    fields.forEach(e => {
        const field = document.getElementById(e);
        if (field) {
            field.innerHTML = '';
            field.style.display = 'none';
        }
    })
    const data = document.getElementById('output');
    if (data) { data.innerHTML = ''}
    if (keepButton) {
        isOpenFile();
    }
}

let tokenizerText: string = '';
function regexExpresion(listOfExpressions: string[]) : Tokenizer[]{
    let tokenizerList: Tokenizer[] = [];
    listOfExpressions.forEach(line => {
        const split = line.split(/( \, )/).filter(a => a !== " , ");
        let tokenize =  {
            regex : new RegExp(split[0]),
            value : split[1] ? split[1] : 'Signo desconocido',
            description : split[2] ? split[2] : ''
        };
        tokenizerList.push(tokenize);
    })
    return tokenizerList;
}

function setterTokenizer(input: any) {
    let file = input.files[0];
    let reader = new FileReader();
    reader.readAsText(file);

    reader.onload = () => {
        if (typeof reader.result === "string") {
            tokenizerText = reader.result;
            regexExpresion(reader.result.split(/\r?\n/gm));
        }
    }

    reader.onerror = () => console.log(reader.error);
}

function sourceCodeResolver (input: any) {
    let tokenizers: Tokenizer[] = regexExpresion(tokenizerText.split(/\r?\n/gm));
    let tokenizer: HTMLElement | null  = document.getElementById("esteotro");
    if (tokenizer) {
        tokenizer.innerHTML = '';
    }
    let file = input.files[0];
    let reader = new FileReader();
    reader.readAsText(file);
    reader.onload = function() {
        if (typeof reader.result === "string") {
            let word = reader.result.split(/\r?\n/gm);
            word.forEach(line => {
                for (let letter = 0; letter < line.length; letter++) {
                    for (let regex = 0; regex < tokenizers.length; regex ++) {
                        // @ts-ignore
                        if(tokenizers[regex].regex.test(line.charAt(letter))) {
                            if (/ /g.test(line.charAt(letter))) {
                                continue ;
                            }
                            if (tokenizer) {
                                tokenizer.innerHTML += '\n' +
                                    `<div class="panel">
                                        <div class="panel-symbol">${line.charAt(letter)}</div>
                                        <div class="panel-description">
                                            <div class="panel-title">${tokenizers[regex].value} <hr></div>
                                            <div class="panel-info">${tokenizers[regex].description}</div>
                                        </div>
                                    </div>`;
                            }
                            // letter += 1;
                        }
                    }
                }
            });
        }
    }
}

let option: string = 'regex';
function selectOption(selected: string) {
    const scanner = document.getElementById('scan_box');
    const regex = document.getElementById('open');
    const logo1 = document.getElementById('logo1');
    const logo2 = document.getElementById('logo2');
    const panel = document.getElementById('EJR_code');
    if(scanner && panel && logo1 && logo2 && regex) {
        scanner.style.display = selected === 'scan' ? 'flex' : 'none';
        logo1.style.display = selected !== 'scan' ? 'flex' : 'none';
        regex.style.display = selected !== 'scan' ? 'flex' : 'none';
        logo2.style.display = selected === 'scan' ? 'flex' : 'none';
        panel.style.boxShadow = selected === 'scan' ? 'inset -8em 0 8em -11em rgb(70 133 224 / 72%)': 'inset -8em 0 8em -11em rgb(154 205 50 / 72%)';
        if (selected === 'scan' ) {
            closeFile();
        }
    }
    option = selected;
}
