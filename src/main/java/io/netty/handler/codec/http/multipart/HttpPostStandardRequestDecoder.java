//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.HttpPostBodyUtil.SeekAheadOptimize;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.MultiPartStatus;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.util.internal.ObjectUtil;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 个人的覆写类，用于覆盖"&","="报错的问题
 */
public class HttpPostStandardRequestDecoder implements InterfaceHttpPostRequestDecoder {
    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private final HttpDataFactory factory;
    private final HttpRequest request;
    private final Charset charset;
    private boolean isLastChunk;
    private final List<InterfaceHttpData> bodyListHttpData;
    private final Map<String, List<InterfaceHttpData>> bodyMapHttpData;
    private ByteBuf undecodedChunk;
    private int bodyListHttpDataRank;
    private MultiPartStatus currentStatus;
    private Attribute currentAttribute;
    private boolean destroyed;
    private int discardThreshold;
    
    public HttpPostStandardRequestDecoder(HttpRequest request) {
        this(new DefaultHttpDataFactory(16384L), request, HttpConstants.DEFAULT_CHARSET);
    }
    
    public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request) {
        this(factory, request, HttpConstants.DEFAULT_CHARSET);
    }
    
    public HttpPostStandardRequestDecoder(HttpDataFactory factory, HttpRequest request, Charset charset) {
        this.bodyListHttpData = new ArrayList();
        this.bodyMapHttpData = new TreeMap(CaseIgnoringComparator.INSTANCE);
        this.currentStatus = MultiPartStatus.NOTSTARTED;
        this.discardThreshold = 10485760;
        this.request = (HttpRequest)ObjectUtil.checkNotNull(request, "request");
        this.charset = (Charset)ObjectUtil.checkNotNull(charset, "charset");
        this.factory = (HttpDataFactory)ObjectUtil.checkNotNull(factory, "factory");
        if (request instanceof HttpContent) {
            this.offer((HttpContent)request);
        } else {
            this.undecodedChunk = Unpooled.buffer();
            this.parseBody();
        }
        
    }
    
    private void checkDestroyed() {
        if (this.destroyed) {
            throw new IllegalStateException(HttpPostStandardRequestDecoder.class.getSimpleName() + " was destroyed already");
        }
    }
    
    public boolean isMultipart() {
        this.checkDestroyed();
        return false;
    }
    
    public void setDiscardThreshold(int discardThreshold) {
        this.discardThreshold = ObjectUtil.checkPositiveOrZero(discardThreshold, "discardThreshold");
    }
    
    public int getDiscardThreshold() {
        return this.discardThreshold;
    }
    
    public List<InterfaceHttpData> getBodyHttpDatas() {
        this.checkDestroyed();
        if (!this.isLastChunk) {
            throw new NotEnoughDataDecoderException();
        } else {
            return this.bodyListHttpData;
        }
    }
    
    public List<InterfaceHttpData> getBodyHttpDatas(String name) {
        this.checkDestroyed();
        if (!this.isLastChunk) {
            throw new NotEnoughDataDecoderException();
        } else {
            return (List)this.bodyMapHttpData.get(name);
        }
    }
    
    public InterfaceHttpData getBodyHttpData(String name) {
        this.checkDestroyed();
        if (!this.isLastChunk) {
            throw new NotEnoughDataDecoderException();
        } else {
            List<InterfaceHttpData> list = (List)this.bodyMapHttpData.get(name);
            return list != null ? (InterfaceHttpData)list.get(0) : null;
        }
    }
    
    public HttpPostStandardRequestDecoder offer(HttpContent content) {
        this.checkDestroyed();
        ByteBuf buf = content.content();
        if (this.undecodedChunk == null) {
            this.undecodedChunk = buf.copy();
        } else {
            this.undecodedChunk.writeBytes(buf);
        }
        
        if (content instanceof LastHttpContent) {
            this.isLastChunk = true;
        }
        
        this.parseBody();
        if (this.undecodedChunk != null && this.undecodedChunk.writerIndex() > this.discardThreshold) {
            this.undecodedChunk.discardReadBytes();
        }
        
        return this;
    }
    
    public boolean hasNext() {
        this.checkDestroyed();
        if (this.currentStatus == MultiPartStatus.EPILOGUE && this.bodyListHttpDataRank >= this.bodyListHttpData.size()) {
            throw new EndOfDataDecoderException();
        } else {
            return !this.bodyListHttpData.isEmpty() && this.bodyListHttpDataRank < this.bodyListHttpData.size();
        }
    }
    
    public InterfaceHttpData next() {
        this.checkDestroyed();
        return this.hasNext() ? (InterfaceHttpData)this.bodyListHttpData.get(this.bodyListHttpDataRank++) : null;
    }
    
    public InterfaceHttpData currentPartialHttpData() {
        return this.currentAttribute;
    }
    
    private void parseBody() {
        if (this.currentStatus != MultiPartStatus.PREEPILOGUE && this.currentStatus != MultiPartStatus.EPILOGUE) {
            this.parseBodyAttributes();
        } else {
            if (this.isLastChunk) {
                this.currentStatus = MultiPartStatus.EPILOGUE;
            }
            
        }
    }
    
    protected void addHttpData(InterfaceHttpData data) {
        if (data != null) {
            List<InterfaceHttpData> datas = (List)this.bodyMapHttpData.get(data.getName());
            if (datas == null) {
                datas = new ArrayList(1);
                this.bodyMapHttpData.put(data.getName(), datas);
            }
            
            ((List)datas).add(data);
            this.bodyListHttpData.add(data);
        }
    }
    
    private void parseBodyAttributesStandard() {
        int firstpos = this.undecodedChunk.readerIndex();
        int currentpos = firstpos;
        if (this.currentStatus == MultiPartStatus.NOTSTARTED) {
            this.currentStatus = MultiPartStatus.DISPOSITION;
        }
        
        boolean contRead = true;
        
        try {
            while(this.undecodedChunk.isReadable() && contRead) {
                char read = (char)this.undecodedChunk.readUnsignedByte();
                ++currentpos;
                int ampersandpos;
                switch(this.currentStatus) {
                    case DISPOSITION:
                        String key;
                        if (read == '=') {
                            this.currentStatus = MultiPartStatus.FIELD;
                            int equalpos = currentpos - 1;
                            key = decodeAttribute(this.undecodedChunk.toString(firstpos, equalpos - firstpos, this.charset), this.charset);
                            this.currentAttribute = this.factory.createAttribute(this.request, key);
                            firstpos = currentpos;
                        } else if (read == '&') {
                            this.currentStatus = MultiPartStatus.DISPOSITION;
                            ampersandpos = currentpos - 1;
                            key = decodeAttribute(this.undecodedChunk.toString(firstpos, ampersandpos - firstpos, this.charset), this.charset);
                            this.currentAttribute = this.factory.createAttribute(this.request, key);
                            this.currentAttribute.setValue("");
                            this.addHttpData(this.currentAttribute);
                            this.currentAttribute = null;
                            firstpos = currentpos;
                            contRead = true;
                        }
                        break;
                    case FIELD:
                        if (read == '&') {
                            this.currentStatus = MultiPartStatus.DISPOSITION;
                            ampersandpos = currentpos - 1;
                            this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                            firstpos = currentpos;
                            contRead = true;
                        } else if (read == '\r') {
                            if (this.undecodedChunk.isReadable()) {
                                read = (char)this.undecodedChunk.readUnsignedByte();
                                ++currentpos;
                                if (read != '\n') {
                                    throw new ErrorDataDecoderException("Bad end of line");
                                }
                                
                                this.currentStatus = MultiPartStatus.PREEPILOGUE;
                                ampersandpos = currentpos - 2;
                                this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                                firstpos = currentpos;
                                contRead = false;
                            } else {
                                --currentpos;
                            }
                        } else if (read == '\n') {
                            this.currentStatus = MultiPartStatus.PREEPILOGUE;
                            ampersandpos = currentpos - 1;
                            this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                            firstpos = currentpos;
                            contRead = false;
                        }
                        break;
                    default:
                        contRead = false;
                }
            }
            
            if (this.isLastChunk && this.currentAttribute != null) {
                if (currentpos > firstpos) {
                    this.setFinalBuffer(this.undecodedChunk.copy(firstpos, currentpos - firstpos));
                } else if (!this.currentAttribute.isCompleted()) {
                    this.setFinalBuffer(Unpooled.EMPTY_BUFFER);
                }
                
                firstpos = currentpos;
                this.currentStatus = MultiPartStatus.EPILOGUE;
            } else if (contRead && this.currentAttribute != null && this.currentStatus == MultiPartStatus.FIELD) {
                this.currentAttribute.addContent(this.undecodedChunk.copy(firstpos, currentpos - firstpos), false);
                firstpos = currentpos;
            }
            
            this.undecodedChunk.readerIndex(firstpos);
        } catch (ErrorDataDecoderException var8) {
            this.undecodedChunk.readerIndex(firstpos);
            throw var8;
        } catch (IOException var9) {
            this.undecodedChunk.readerIndex(firstpos);
            throw new ErrorDataDecoderException(var9);
        }
    }
    
    private void parseBodyAttributes() {
        if(!this.request.headers().get("Content-Type").startsWith(APPLICATION_X_WWW_FORM_URLENCODED)){
            return;
        }
        if (!this.undecodedChunk.hasArray()) {
            this.parseBodyAttributesStandard();
        } else {
            SeekAheadOptimize sao = new SeekAheadOptimize(this.undecodedChunk);
            int firstpos = this.undecodedChunk.readerIndex();
            int currentpos = firstpos;
            if (this.currentStatus == MultiPartStatus.NOTSTARTED) {
                this.currentStatus = MultiPartStatus.DISPOSITION;
            }
            
            boolean contRead = true;
            
            try {
                label81:
                while(sao.pos < sao.limit) {
                    char read = (char)(sao.bytes[sao.pos++] & 255);
                    ++currentpos;
                    int ampersandpos;
                    switch(this.currentStatus) {
                        case DISPOSITION:
                            String key;
                            if (read == '=') {
                                this.currentStatus = MultiPartStatus.FIELD;
                                int equalpos = currentpos - 1;
                                key = decodeAttribute(this.undecodedChunk.toString(firstpos, equalpos - firstpos, this.charset), this.charset);
                                this.currentAttribute = this.factory.createAttribute(this.request, key);
                                firstpos = currentpos;
                            } else if (read == '&') {
                                this.currentStatus = MultiPartStatus.DISPOSITION;
                                ampersandpos = currentpos - 1;
                                key = decodeAttribute(this.undecodedChunk.toString(firstpos, ampersandpos - firstpos, this.charset), this.charset);
                                this.currentAttribute = this.factory.createAttribute(this.request, key);
                                this.currentAttribute.setValue("");
                                this.addHttpData(this.currentAttribute);
                                this.currentAttribute = null;
                                firstpos = currentpos;
                                contRead = true;
                            }
                            break;
                        case FIELD:
                            if (read == '&') {
                                this.currentStatus = MultiPartStatus.DISPOSITION;
                                ampersandpos = currentpos - 1;
                                this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                                firstpos = currentpos;
                                contRead = true;
                            } else if (read == '\r') {
                                if (sao.pos < sao.limit) {
                                    read = (char)(sao.bytes[sao.pos++] & 255);
                                    ++currentpos;
                                    if (read != '\n') {
                                        sao.setReadPosition(0);
                                        throw new ErrorDataDecoderException("Bad end of line");
                                    }
                                    
                                    this.currentStatus = MultiPartStatus.PREEPILOGUE;
                                    ampersandpos = currentpos - 2;
                                    sao.setReadPosition(0);
                                    this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                                    firstpos = currentpos;
                                    contRead = false;
                                    break label81;
                                }
                                
                                if (sao.limit > 0) {
                                    --currentpos;
                                }
                            } else {
                                if (read != '\n') {
                                    continue;
                                }
                                
                                this.currentStatus = MultiPartStatus.PREEPILOGUE;
                                ampersandpos = currentpos - 1;
                                sao.setReadPosition(0);
                                this.setFinalBuffer(this.undecodedChunk.copy(firstpos, ampersandpos - firstpos));
                                firstpos = currentpos;
                                contRead = false;
                                break label81;
                            }
                            break;
                        default:
                            sao.setReadPosition(0);
                            contRead = false;
                            break label81;
                    }
                }
                
                if (this.isLastChunk && this.currentAttribute != null) {
                    if (currentpos > firstpos) {
                        this.setFinalBuffer(this.undecodedChunk.copy(firstpos, currentpos - firstpos));
                    } else if (!this.currentAttribute.isCompleted()) {
                        this.setFinalBuffer(Unpooled.EMPTY_BUFFER);
                    }
                    
                    firstpos = currentpos;
                    this.currentStatus = MultiPartStatus.EPILOGUE;
                } else if (contRead && this.currentAttribute != null && this.currentStatus == MultiPartStatus.FIELD) {
                    this.currentAttribute.addContent(this.undecodedChunk.copy(firstpos, currentpos - firstpos), false);
                    firstpos = currentpos;
                }
                
                this.undecodedChunk.readerIndex(firstpos);
            } catch (ErrorDataDecoderException var9) {
                this.undecodedChunk.readerIndex(firstpos);
                throw var9;
            } catch (IOException var10) {
                this.undecodedChunk.readerIndex(firstpos);
                throw new ErrorDataDecoderException(var10);
            } catch (IllegalArgumentException var11) {
                this.undecodedChunk.readerIndex(firstpos);
                throw new ErrorDataDecoderException(var11);
            }
        }
    }
    
    private void setFinalBuffer(ByteBuf buffer) throws IOException {
        this.currentAttribute.addContent(buffer, true);
        String value = decodeAttribute(this.currentAttribute.getByteBuf().toString(this.charset), this.charset);
        this.currentAttribute.setValue(value);
        this.addHttpData(this.currentAttribute);
        this.currentAttribute = null;
    }
    
    private static String decodeAttribute(String s, Charset charset) {
        try {
            return QueryStringDecoder.decodeComponent(s, charset);
        } catch (IllegalArgumentException var3) {
            throw new ErrorDataDecoderException("Bad string: '" + s + '\'', var3);
        }
    }
    
    public void destroy() {
        this.cleanFiles();
        this.destroyed = true;
        if (this.undecodedChunk != null && this.undecodedChunk.refCnt() > 0) {
            this.undecodedChunk.release();
            this.undecodedChunk = null;
        }
        
    }
    
    public void cleanFiles() {
        this.checkDestroyed();
        this.factory.cleanRequestHttpData(this.request);
    }
    
    public void removeHttpDataFromClean(InterfaceHttpData data) {
        this.checkDestroyed();
        this.factory.removeHttpDataFromClean(this.request, data);
    }
}
